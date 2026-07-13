package com.marketplace.payment;

import com.marketplace.payment.outbox.PaymentPayload;
import com.marketplace.reservation.ReservationRepository;
import com.marketplace.reservation.ReservationStatus;
import com.marketplace.reservation.exception.ReservationExpiredException;
import com.marketplace.reservation.exception.ReservationNotFoundException;
import com.marketplace.payment.dto.PaymentInitiateResponse;
import com.marketplace.order.exception.PaymentAlreadyCompleteException;
import com.marketplace.order.exception.PaymentAlreadyInitiatedException;
import com.marketplace.outbox.OutboxEvent;
import com.marketplace.outbox.OutboxEventRepository;
import com.marketplace.outbox.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final ReservationRepository reservationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    PaymentInitiateResponse initiatePayment(Long reservationId) {
        int updated = reservationRepository.markAsPaymentInitiated(reservationId);

        if (updated == 0) {
            Optional<ReservationStatus> inventoryReservationStatusOptional =
                    reservationRepository.findStatusById(reservationId);
            if (inventoryReservationStatusOptional.isEmpty()) {
                throw new ReservationNotFoundException(reservationId);
            }
            switch (inventoryReservationStatusOptional.get()) {
                case PAYMENT_INITIATED -> throw new PaymentAlreadyInitiatedException();
                case PAID -> throw new PaymentAlreadyCompleteException(); // TODO: return order ID
                case EXPIRED -> throw new ReservationExpiredException(reservationId);
                default -> throw new IllegalStateException("Unhandled reservation status case");
            }
        }
        PaymentPayload paymentPayload = new PaymentPayload(reservationId);
        String payloadAsString = objectMapper.writeValueAsString(paymentPayload);
        OutboxEvent paymentEvent = new OutboxEvent(OutboxEventType.PAYMENT, payloadAsString);

        outboxEventRepository.save(paymentEvent);

        return new PaymentInitiateResponse();
    }
}
