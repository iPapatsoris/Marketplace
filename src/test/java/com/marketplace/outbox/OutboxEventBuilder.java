package com.marketplace.outbox;

import com.marketplace.payment.outbox.PaymentPayload;
import tools.jackson.databind.ObjectMapper;

import static com.marketplace.outbox.OutboxEventType.PAYMENT;

public class OutboxEventBuilder {
    private Long id = 1L;
    private OutboxEventType type = PAYMENT;

    private ObjectMapper objectMapper = new ObjectMapper();

    public OutboxEvent build() {
        PaymentPayload paymentPayload = new PaymentPayload(10L);
        String payloadAsString = objectMapper.writeValueAsString(paymentPayload);

        return new OutboxEvent(type, payloadAsString);
    }

    public OutboxEventBuilder withId(Long id) {
        this.id = id;
        return this;
    }
}
