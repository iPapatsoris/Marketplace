package com.marketplace.payment;

import com.marketplace.annotations.SpringIntegrationTest;
import com.marketplace.order.Order;
import com.marketplace.order.OrderRepository;
import com.marketplace.outbox.OutboxEvent;
import com.marketplace.outbox.OutboxEventRepository;
import com.marketplace.outbox.OutboxEventService;
import com.marketplace.outbox.OutboxEventStatus;
import com.marketplace.payment.dto.PaymentInitiateResponse;
import com.marketplace.payment.provider.PaymentProvider;
import com.marketplace.payment.provider.PaymentResult;
import com.marketplace.product.Product;
import com.marketplace.product.ProductBuilder;
import com.marketplace.product.ProductSnapshot;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.reservation.ReservationRepository;
import com.marketplace.reservation.ReservationService;
import com.marketplace.reservation.entity.Reservation;
import com.marketplace.reservation.entity.ReservationBuilder;
import com.marketplace.reservation.entity.ReservationStatus;
import com.marketplace.util.MutableClock;
import jakarta.persistence.EntityManager;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static com.marketplace.util.ConcurrencyControl.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringIntegrationTest
public class PaymentIntegrationTest {

    @TestBean
    Clock clock;

    static Clock clock() {
        return new MutableClock(
                Instant.parse("2026-01-01T00:00:00Z"),
                ZoneOffset.UTC
        );
    }

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxEventService outboxEventService;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private PaymentProvider paymentProvider;

    private Product product;
    private Reservation reservation;

    @BeforeEach
    void setup() {
        product = new ProductBuilder().build();
        reservation = new ReservationBuilder()
                .withProduct(product)
                .withProductSnapshot(objectMapper.writeValueAsString(
                        new ProductSnapshot(product.getName(), product.getPrice())))
                .build();
        productRepository.save(product);
        reservationRepository.save(reservation);
    }

    @AfterEach
    void cleanup() {
        // no way to automatically rollback everything? could forget some side effect
        reservationRepository.deleteAll();
        productRepository.deleteAll();
        orderRepository.deleteAll();
        outboxEventRepository.deleteAll();
    }

    private PaymentInitiateResponse startPayment() {
        return restClient.post().uri("/reservations/{id}/payment", reservation.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentInitiateResponse.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    void shouldCompleteOrder() {
        var response = startPayment();
        verifyNoInteractions(paymentProvider);
        assertThat(response.status()).isEqualTo(ReservationStatus.PAYMENT_INITIATED);

        when(paymentProvider.complete()).thenReturn(PaymentResult.SUCCESS);
        outboxEventService.processEvents();

        Reservation persistedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(persistedReservation.getStatus()).isEqualTo(ReservationStatus.PAID);
        assertThat(persistedReservation.getOrderId()).isNotNull();

        Order persistedOrder = orderRepository.findById(persistedReservation.getId()).orElseThrow();
        assertThat(persistedOrder.getReservationId()).isEqualTo(persistedReservation.getId());
        assertThat(persistedOrder.getProductSnapshot()).isEqualTo(persistedReservation.getProductSnapshot());

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        OutboxEvent event = events.getFirst();
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isEqualTo(Instant.now(clock));
    }

    @Test
    void shouldAllowUserRetryOnPaymentDecline() {
        var response = startPayment();
        assertThat(response.status()).isEqualTo(ReservationStatus.PAYMENT_INITIATED);

        // Assume payment decline
        when(paymentProvider.complete()).thenReturn(PaymentResult.DECLINED);
        outboxEventService.processEvents();

        // Assert reservation is still active and no order was created
        Reservation persistedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(persistedReservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(persistedReservation.getOrderId()).isNull();
        assertThat(orderRepository.findAll()).isEmpty();

        // Assert event has been processed
        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        OutboxEvent event = events.getFirst();
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isEqualTo(Instant.now(clock));

        // Attempt to pay again
        response = startPayment();
        assertThat(response.status()).isEqualTo(ReservationStatus.PAYMENT_INITIATED);

        // Assume payment success this time
        when(paymentProvider.complete()).thenReturn(PaymentResult.SUCCESS);
        outboxEventService.processEvents();
        entityManager.clear();

        // Assert successful payment
        persistedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(persistedReservation.getStatus()).isEqualTo(ReservationStatus.PAID);
        assertThat(persistedReservation.getOrderId()).isNotNull();

        events = outboxEventRepository.findAll();
        assertThat(events)
                .hasSize(2)
                .allMatch(e -> e.getStatus() == OutboxEventStatus.PROCESSED);
    }

    @Test
    void shouldRetryOnTransientError() {
        var response = startPayment();
        assertThat(response.status()).isEqualTo(ReservationStatus.PAYMENT_INITIATED);

        // Assume transient error
        when(paymentProvider.complete()).thenThrow(new RuntimeException());
        outboxEventService.processEvents();

        // Assert reservation is still under payment
        Reservation persistedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(persistedReservation.getStatus()).isEqualTo(ReservationStatus.PAYMENT_INITIATED);
        assertThat(persistedReservation.getOrderId()).isNull();
        assertThat(orderRepository.findAll()).isEmpty();

        // Assert event is still pending
        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        OutboxEvent event = events.getFirst();
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isAfter(Instant.now(clock));

        // Advance time to event's next processing time
        ((MutableClock) clock).set(event.getNextAttemptAt());

        // Assume successful payment this time
        doReturn(PaymentResult.SUCCESS).when(paymentProvider).complete();
        outboxEventService.processEvents();
        entityManager.clear();

        // Assert successful payment
        persistedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(persistedReservation.getStatus()).isEqualTo(ReservationStatus.PAID);
        assertThat(persistedReservation.getOrderId()).isNotNull();

        events = outboxEventRepository.findAll();
        assertThat(events)
                .hasSize(1)
                .allMatch(e -> e.getStatus() == OutboxEventStatus.PROCESSED
                                            && e.getAttempts() == 1);
    }

    @Test
    void shouldTerminallyFailOnReservationDeletion() {
        var response = startPayment();
        assertThat(response.status()).isEqualTo(ReservationStatus.PAYMENT_INITIATED);

        reservationRepository.deleteAll();

        when(paymentProvider.complete()).thenReturn(PaymentResult.SUCCESS);
        outboxEventService.processEvents();

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);
        OutboxEvent event = events.getFirst();
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getAttempts()).isEqualTo(0);
        assertThat(event.getNextAttemptAt()).isNull();
    }

    @Test
    void shouldNotExpireReservationAfterPaymentStarted(@Autowired ReservationService reservationService) {
        var response = startPayment();
        assertThat(response.status()).isEqualTo(ReservationStatus.PAYMENT_INITIATED);

        Reservation persistedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(persistedReservation.getExpiresAt()).isAfter(Instant.now(clock));

        // Advance time to after expiration
        ((MutableClock) clock).set(persistedReservation.getExpiresAt().plus(2, ChronoUnit.MINUTES));

        // Attempt expiration
        reservationService.expireReservations();
        entityManager.clear();

        // Assert reservation hasn't expired
        persistedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(persistedReservation.getStatus()).isEqualTo(ReservationStatus.PAYMENT_INITIATED);

        // Finish payment
        outboxEventService.processEvents();

        // Attempt expiration
        reservationService.expireReservations();
        entityManager.clear();

        // Assert reservation is paid
        persistedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(persistedReservation.getStatus()).isEqualTo(ReservationStatus.PAID);
    }

    @Test
    void shouldNotAllowDuplicatePaymentAttempts() {
        int requestCount = 10;
        EntityExchangeResult<byte[]>[] responses = new EntityExchangeResult[requestCount];
        Future<?>[] threadResults = new Future[requestCount];

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CyclicBarrier barrier = new CyclicBarrier(requestCount);

        Consumer<Integer> worker = index -> {
            await(barrier);
            responses[index] = restClient.post().uri("/reservations/{id}/payment", reservation.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectBody()
                    .returnResult();
        };

        // Start payment multiple times
        try {
            for (int i = 0; i < requestCount; i++) {
                final int requestIndex = i;
                Runnable thread = () -> {
                    worker.accept(requestIndex);
                };
                threadResults[i] = executor.submit(thread);
            }

            for (Future<?> result : threadResults) {
                result.get();
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new AssertionFailure("Worker thread failed", e.getCause());
        }
        // No shutdown/shutdownNow needed.
        // If a worker fails, Future.get() returns immediately with an ExecutionException,
        // while the remaining workers complete naturally. This test has no long-running
        // or blocking work that requires interruption.

        var successes =  Arrays.stream(responses).filter(
                response -> response.getStatus() == HttpStatus.OK
        ).toList();
        var failures =  Arrays.stream(responses).filter(
                response -> response.getStatus() == HttpStatus.CONFLICT
        ).toList();

        // Assert only 1 request succeeds
        assertThat(successes).hasSize(1);
        assertThat(failures).hasSize(requestCount - 1);
        assertThat(failures).allMatch(failure -> {
            ProblemDetail problemDetail = objectMapper.readValue(
                    failure.getResponseBody(), ProblemDetail.class);
            return problemDetail.getTitle().equals("Payment already initiated");
        });

        PaymentInitiateResponse successResponse = objectMapper.readValue(
                successes.getFirst().getResponseBody(), PaymentInitiateResponse.class);
        assertThat(successResponse.status()).isEqualTo(ReservationStatus.PAYMENT_INITIATED);

        // Complete payment
        outboxEventService.processEvents();

        assertThat(orderRepository.findAll()).hasSize(1);

        // Ensure the actual payment provider was not called multiple times
        verify(paymentProvider, times(1)).complete();
    }


    @Test
    void shouldNotAllowPaymentIfAlreadyComplete() {
        PaymentInitiateResponse response = startPayment();
        assertThat(response.status()).isEqualTo(ReservationStatus.PAYMENT_INITIATED);

        when(paymentProvider.complete()).thenReturn(PaymentResult.SUCCESS);
        outboxEventService.processEvents();

        ProblemDetail problemDetail =  restClient.post().uri("/reservations/{id}/payment", reservation.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();
        assertThat(problemDetail.getTitle()).isEqualTo("Payment already complete");
    }

    @Test
    void shouldNotAllowPaymentIfReservationExpired() {
        Reservation persistedReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
        persistedReservation.setStatus(ReservationStatus.EXPIRED);
        reservationRepository.save(persistedReservation);

        ProblemDetail response = restClient.post().uri("/reservations/{id}/payment", reservation.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();
        assertThat(response.getTitle()).isEqualTo("Reservation expired");
    }
}
