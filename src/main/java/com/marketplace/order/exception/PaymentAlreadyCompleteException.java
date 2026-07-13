package com.marketplace.order.exception;

public class PaymentAlreadyCompleteException extends RuntimeException {
    public PaymentAlreadyCompleteException() {
        super("Payment has already been completed");
    }
}
