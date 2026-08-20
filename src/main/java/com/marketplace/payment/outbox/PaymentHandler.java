package com.marketplace.payment.outbox;

import com.marketplace.payment.provider.PaymentProvider;
import com.marketplace.payment.provider.PaymentResult;
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
    private final PaymentProvider paymentProvider;

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
        Long reservationId = payload.reservationId();

        PaymentResult paymentResult;
        try {
            paymentResult = paymentProvider.complete();
        } catch (Exception ex) {
            throw new PaymentTransientException(reservationId);
        }

        if (paymentResult == PaymentResult.DECLINED) {
            // Revert reservation to ACTIVE
            // Give the user more chances until it expires
            // Do NOT rollback event, let it be marked as PROCESSED
            // If they re-attempt payment, a new one event will be created
            reservationRepository.markAsActive(reservationId);
            return;
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
