package com.marketplace.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.TemporalAmount;

@Component
@RequiredArgsConstructor
public class OutboxEventService {
    private final Clock clock;

    public void markProcessed(OutboxEvent outboxEvent) {
        outboxEvent.markProcessed(Instant.now(clock));
    }

    public void markFailed(OutboxEvent outboxEvent) {
        outboxEvent.markFailed(Instant.now(clock));
    }

    public void scheduleRetry(OutboxEvent outboxEvent, TemporalAmount delay) {
        outboxEvent.scheduleRetry(Instant.now(clock).plus(delay));
    }
}
