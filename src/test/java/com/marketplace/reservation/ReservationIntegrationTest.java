package com.marketplace.reservation;

import com.marketplace.annotations.SpringIntegrationTest;
import com.marketplace.product.Product;
import com.marketplace.product.ProductBuilder;
import com.marketplace.product.ProductSnapshot;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.reservation.dto.CreateReservationRequest;
import com.marketplace.reservation.dto.CreateReservationResponse;
import com.marketplace.reservation.entity.Reservation;
import com.marketplace.reservation.entity.ReservationStatus;
import com.marketplace.util.MutableClock;
import jakarta.persistence.EntityManager;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static com.marketplace.util.ConcurrencyControl.await;
import static org.assertj.core.api.Assertions.assertThat;

@SpringIntegrationTest
public class ReservationIntegrationTest {

    @TestBean
    private Clock clock;

    static Clock clock() {
        return new MutableClock(
                Instant.parse("2026-01-01T00:00:00Z"),
                ZoneOffset.UTC
        );
    }

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private  EntityManager entityManager;

    private Product product;
    private CreateReservationRequest request;

    @BeforeEach
    public void setup() {
        product = new ProductBuilder()
                .withVersion(10L)
                .withName("chair")
                .withPrice(new BigDecimal("30.00"))
                .withInventory(10)
                .build();
        request = new CreateReservationRequest(2, product.getVersion());
        product = productRepository.save(product);
    }

    @AfterEach
    void cleanup() {
        reservationRepository.deleteAll();
        productRepository.deleteAll();
    }

    private CreateReservationResponse postReservation(CreateReservationRequest request) {
        return restClient.post().uri("/products/{id}/reservations", product.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CreateReservationResponse.class)
                .returnResult().getResponseBody();
    }

    @Test
    void shouldCreateReservation() {
        CreateReservationResponse response = postReservation(request);
        assertThat(response.quantity()).isEqualTo(request.quantity());

        // Assert reservation went through
        Reservation persistedReservation = reservationRepository.findById(response.reservationID()).orElseThrow();
        assertThat(persistedReservation.getProduct().getId()).isEqualTo(product.getId());
        assertThat(persistedReservation.getQuantity()).isEqualTo(request.quantity());
        assertThat(persistedReservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);

        // Assert product snapshot matches product
        ProductSnapshot snapshot = objectMapper.readValue(persistedReservation.getProductSnapshot(), ProductSnapshot.class);
        assertThat(snapshot.name()).isEqualTo(product.getName());
        assertThat(snapshot.price()).isEqualTo(product.getPrice());

        // Assert product inventory has decreased accordingly
        Product persistedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(persistedProduct.getInventory()).isEqualTo(product.getInventory() - response.quantity());
    }

    @Test
    void shouldExpireReservation(@Autowired ReservationService reservationService) {
        CreateReservationResponse response = postReservation(request);
        Duration expirationOffset = Duration.of(10, ChronoUnit.MINUTES); // TODO: import instead of hardcoding
        assertThat(response.expiresAt()).isEqualTo(clock.instant().plus(expirationOffset));

        // Attempt expiration before it's due
        reservationService.expireReservations();

        // Assert reservation remained active
        Reservation persistedReservation = reservationRepository.findById(response.reservationID()).orElseThrow();
        Product persistedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(persistedReservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(persistedProduct.getInventory()).isEqualTo(
                product.getInventory() - persistedReservation.getQuantity());

        // Advance time and expire
        entityManager.clear();
        ((MutableClock) clock).advance(expirationOffset);
        reservationService.expireReservations();

        // Assert expiration
        persistedReservation = reservationRepository.findById(response.reservationID()).orElseThrow();
        persistedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(persistedReservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(persistedProduct.getInventory()).isEqualTo(product.getInventory());
    }

    @Test
    @Disabled("""
                  Concurrent reservation requests fail due to optimistic locking conflict on product's inventory changing.
                  Enable test after moving inventory to a separate table to exclude it from versioning""")
    void shouldNotOverReserveOnConcurrent() {
        int requestCount = 15;
        int quantityPerRequest = 3;
        int expectedSuccesses = product.getInventory() / quantityPerRequest;
        CreateReservationRequest[] requests = new CreateReservationRequest[requestCount];
        EntityExchangeResult<byte[]>[] responses = new EntityExchangeResult[requestCount];
        Future<?>[] threadResults = new Future[requestCount];

        for (int i = 0; i < requestCount; i++) {
            requests[i] = new CreateReservationRequest(
                            quantityPerRequest,
                            product.getVersion()
                    );
        }

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CyclicBarrier barrier = new CyclicBarrier(requestCount);

        Consumer<Integer> worker = index -> {
            await(barrier);
            CreateReservationRequest request = requests[index];
            responses[index] = restClient.post().uri("/products/{id}/reservations", product.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange()
                    .expectBody()
                    .returnResult();
        };

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

        Predicate<EntityExchangeResult<byte[]>> isFailedUpdate =
                response -> {
                    if (response.getStatus() != HttpStatus.CONFLICT) {
                        return false;
                    }
                    ProblemDetail responseDto = objectMapper.readValue(
                            response.getResponseBody(), ProblemDetail.class);
                    return responseDto.getTitle().equals("Insufficient stock");
                };

        var successIndexes = IntStream.range(0, requestCount)
                .filter(i -> responses[i].getStatus() == HttpStatus.OK)
                .toArray();

        // Assert number of successes and failures
        assertThat(successIndexes).hasSize(expectedSuccesses);
        assertThat(responses).filteredOn(isFailedUpdate).hasSize(requestCount - expectedSuccesses);

        // Assert appropriate inventory reduction
        Product persistedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(persistedProduct.getInventory()).isEqualTo(product.getInventory() % quantityPerRequest);
    }

}
