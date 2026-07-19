package com.marketplace.payment.dto;

import com.marketplace.reservation.entity.ReservationStatus;

public record PaymentInitiateResponse(ReservationStatus status) {
    public PaymentInitiateResponse() {
        this(ReservationStatus.PAYMENT_INITIATED);
    }
}
