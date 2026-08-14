package com.marketplace.outbox;

import com.marketplace.outbox.exception.OutboxEventFailureException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.Optional;

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

    private final OutboxEventRepository outboxRepository;
    private final OutboxEventHandlerRegistry registry;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Collects pending jobs and
     * runs each on their own transaction. This ensures that if an unhandled exception occurs in one,
     * only that one will roll back, without losing previous or future work on others.
     * After rollback, the exception is caught outside the transaction and another transaction is created to
     * schedule a retry. The retry scheduling updates error log counters and reschedules the job in the future with
     * a delay, to prevent starvation and allow others to complete.
     * Note that in case of a server crash before or during the retry scheduling, the job remains ACTIVE and will
     * be picked up again by the next iteration of the method; it's only the error logging and rescheduling delay
     * that will be lost.
     *
     * If a handler raises OutboxEventFailureException, the job will be marked as terminally FAILED with no roll back
     * and no retry scheduling.
     *
     * In a distributed environment in which the method can run from multiple app instances,
     * each instance may retrieve overlapping pending jobs for execution, but no actual duplicate work will be
     * made. This is because each transaction holds a lock on the job it's processing, with SKIP LOCKED
     * so that other instances will skip locked jobs and move to the next available one.
     */
    public void processEvents() {
        List<Long> eventsIds = outboxRepository.findPending(Instant.now(clock));

        for (Long eventId : eventsIds) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    processEvent(eventId);
                });
            } catch (RuntimeException e) {
                transactionTemplate.executeWithoutResult(status -> {
                    scheduleRetry(eventId);
                });
            }
        }
    }

    private void processEvent(Long eventId) {
        Optional<OutboxEvent> optionalEvent = outboxRepository.findAndLockSingleEventSkipLocked(eventId);
        if (optionalEvent.isEmpty()) {
            System.out.printf("Event with id %d already being processed or doesn't exist, skipping%n", eventId); // switch to a warning log
            return;
        }

        OutboxEvent event = optionalEvent.get();

        try {
            runHandler(event);
        } catch (OutboxEventFailureException ex) {
            markFailed(event);
            return;
        }
        markProcessed(event);
    }

    private void scheduleRetry(Long eventId) {
        Optional<OutboxEvent> failedEventOptional = outboxRepository.findById(eventId);
        if (failedEventOptional.isEmpty()) {
            System.out.printf("Event with id %d not found%n", eventId); // switch to a warning log
            return;
        }
        scheduleRetry(
                failedEventOptional.get(),
                Duration.ofMinutes(1)
        );
    }

    @SuppressWarnings("unchecked")
    private <T> void runHandler(OutboxEvent event) {
        OutboxEventHandler<T> handler = (OutboxEventHandler<T>) registry.get(event.getType());
        T payload = objectMapper.readValue(event.getPayload(), handler.payloadClass());

        handler.handle(payload);
    }
}
