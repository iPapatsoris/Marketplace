package com.marketplace.outbox.exception;

public class OutboxEventFailureException extends RuntimeException {
    public OutboxEventFailureException(String message) {
        super(message);
    }
}
