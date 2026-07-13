package com.marketplace.outbox.exception;

public class OutboxEventNotFoundException extends RuntimeException {
    public OutboxEventNotFoundException(Long id) {
        super("Event with id %d not found".formatted(id));
    }
}
