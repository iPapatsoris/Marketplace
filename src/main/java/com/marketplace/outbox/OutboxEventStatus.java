package com.marketplace.outbox;

public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED
}
