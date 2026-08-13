package com.marketplace.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

@RequiredArgsConstructor
public class OutboxEventProcessorJob {
    private final OutboxEventService outboxEventService;

    @Scheduled(fixedDelay = 5000)
    public void process() {
        outboxEventService.process();
    }
}
