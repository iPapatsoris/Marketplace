package com.marketplace.outbox;

// DUMMY event types are used only in testing
// TODO: find an alternative to remove them and make them local to testing
public enum OutboxEventType {
    PAYMENT, DUMMY1, DUMMY2, DUMMY3
}
