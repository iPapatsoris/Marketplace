package com.marketplace.payment;

import com.marketplace.outbox.OutboxEvent;
import com.marketplace.outbox.OutboxEventRepository;
import com.marketplace.payment.dto.PaymentInitiateResponse;
import com.marketplace.reservation.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static com.marketplace.reservation.entity.ReservationStatus.PAYMENT_INITIATED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {
    private PaymentService paymentService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Long reservationID = 1L;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setup() {
        paymentService = new PaymentService(reservationRepository, outboxEventRepository, objectMapper);
    }

    @Test
    void shouldInitiatePayment() {
        when(reservationRepository.markAsPaymentInitiated(reservationID))
                .thenReturn(1);
        PaymentInitiateResponse response = paymentService.initiatePayment(reservationID);
        verify(outboxEventRepository).save(any(OutboxEvent.class));
        assertEquals(PAYMENT_INITIATED, response.status());
    }

    // TODO: Add error paths..
}
