package com.marketplace.outbox;

public enum OutboxEventStatus {
    PENDING,
    PROCESSED,
    FAILED
}
