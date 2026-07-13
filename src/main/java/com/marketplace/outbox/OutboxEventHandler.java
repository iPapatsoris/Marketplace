package com.marketplace.outbox;

public interface OutboxEventHandler<T> {
    OutboxEventType supports();
    Class<T> payloadClass();

    /**
     * Event handlers can throw {@link com.marketplace.outbox.exception.OutboxEventFailureException}
     * to mark event as terminally FAILED. Any other unhandled exception will result in rollback and a retry
     * being scheduled some time in the future.
     */
    void handle(T payload);
}
