package com.marketplace.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table
@Getter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventType type;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventStatus status = OutboxEventStatus.PENDING;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private Instant nextAttemptAt = Instant.now();

    private Instant processedAt;

    public OutboxEvent(
            OutboxEventType type,
            String payload
    ) {
        this.type = type;
        this.payload = payload;
    }

    public void markProcessed(Instant processedAt) {
        status = OutboxEventStatus.PROCESSED;
        this.processedAt = processedAt;
    }

    public void markFailed(Instant processedAt) {
        status = OutboxEventStatus.FAILED;
        this.processedAt = processedAt;
    }

    public void scheduleRetry(Instant nextAttemptAt) {
        attempts++;
        this.nextAttemptAt = nextAttemptAt;
    }
}
