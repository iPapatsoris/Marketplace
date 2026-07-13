package com.marketplace.payment.outbox;

import com.marketplace.reservation.ReservationRepository;
import com.marketplace.order.Order;
import com.marketplace.order.OrderRepository;
import com.marketplace.outbox.OutboxEventHandler;
import com.marketplace.outbox.OutboxEventType;
import com.marketplace.outbox.exception.OutboxEventFailureException;
import com.marketplace.payment.exception.PaymentTransientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentHandler implements OutboxEventHandler<PaymentPayload> {
    private final ReservationRepository reservationRepository;
    private final OrderRepository orderRepository;

    @Override
    public OutboxEventType supports() {
        return OutboxEventType.PAYMENT;
    }

    @Override
    public Class<PaymentPayload> payloadClass() {
        return PaymentPayload.class;
    }

    @Override
    public void handle(PaymentPayload payload) {
        System.out.println("Payment handler");
        Long reservationId = payload.reservationId();

        // On user error
        if (false) {
            // Revert reservation to ACTIVE
            // Give the user more chances until it expires
            // Do NOT roll back event; if they re-attempt payment, a new one will be created
            reservationRepository.markAsActive(reservationId);
            return;
        }

        // On transient error
        if (false) {
            // Rollback, schedule retry
            throw new PaymentTransientException(reservationId);
        }

        // On success

        // Would use UPDATE RETURNING if it was supported, to avoid extra SELECT query.
        // Can do it with a custom implementation, but not worth it here.
        // Product snapshot cannot change anyway.
        Optional<String> productSnapshotOptional = reservationRepository.findProductSnapshotById(reservationId);

        if (productSnapshotOptional.isEmpty()) {
            throw new OutboxEventFailureException("Reservation with id %d doesn't exist".formatted(reservationId));
        }

        Order order = new Order(productSnapshotOptional.get(), reservationId);
        order = orderRepository.save(order);
        reservationRepository.markAsPaid(reservationId, order.getId());
    }
}
