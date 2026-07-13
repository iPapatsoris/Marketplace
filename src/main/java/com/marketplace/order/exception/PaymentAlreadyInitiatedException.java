package com.marketplace.order.exception;

public class PaymentAlreadyInitiatedException extends RuntimeException {
    public PaymentAlreadyInitiatedException() {
        super("Payment is already being processed");
    }
}
