package com.marketplace.payment.exception;

public class PaymentTransientException extends RuntimeException {
    public PaymentTransientException(Long id) {
        super("Payment for reservation with id %d encountered a transient error".formatted(id));
    }
}
