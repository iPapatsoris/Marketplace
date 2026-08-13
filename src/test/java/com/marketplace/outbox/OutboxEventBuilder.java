package com.marketplace.outbox;

import com.marketplace.payment.outbox.PaymentPayload;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;

import static com.marketplace.outbox.OutboxEventType.DUMMY1;
import static com.marketplace.outbox.OutboxEventType.PAYMENT;

public class OutboxEventBuilder <T> {
    private OutboxEventType type = DUMMY1;
    private T payload;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OutboxEvent build(Clock clock) {
        String payloadAsString = objectMapper.writeValueAsString(payload);
        return new OutboxEvent(type, payloadAsString, Instant.now(clock));
    }

    public OutboxEventBuilder<T> withType(OutboxEventType type) {
        this.type = type;
        return this;
    }

    public OutboxEventBuilder<T> withPayload(T payload) {
        this.payload = payload;
        return this;
    }
}
