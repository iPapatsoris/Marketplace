package com.marketplace.outbox;

import com.marketplace.outbox.exception.OutboxEventFailureException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OutboxEventProcessor {
    private final OutboxEventRepository outboxRepository;
    private final OutboxEventService outboxEventService;
    private final OutboxEventHandlerRegistry registry;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Scheduled task with a fixed delay that collects pending jobs and
     * runs each on their own transaction. This ensures that if an unhandled exception occurs in one job,
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
    @Scheduled(fixedDelay = 5000)
    public void process() {
        List<Long> eventsIds = outboxRepository.findPending();

        for (Long eventId : eventsIds) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    processEvent(eventId);
                });
            } catch (Exception ex) {
                transactionTemplate.executeWithoutResult(status -> {
                    scheduleRetry(eventId);
                });
            }
        }
    }

    void processEvent(Long eventId) {
        Optional<OutboxEvent> optionalEvent = outboxRepository.findAndLockSingleEventSkipLocked(eventId);
        if (optionalEvent.isEmpty()) {
            System.out.printf("Event with id %d already being processed or doesn't exist, skipping%n", eventId); // switch to a warning log
            return;
        }

        OutboxEvent event = optionalEvent.get();

        try {
            runHandler(event);
        } catch (OutboxEventFailureException ex) {
            outboxEventService.markFailed(event);
            return;
        }
        outboxEventService.markProcessed(event);
    }

    void scheduleRetry(Long eventId) {
        Optional<OutboxEvent> failedEventOptional = outboxRepository.findById(eventId);
        if (failedEventOptional.isEmpty()) {
            System.out.printf("Event with id %d not found%n", eventId); // switch to a warning log
            return;
        }
        outboxEventService.scheduleRetry(
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
