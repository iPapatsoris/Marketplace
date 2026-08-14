package com.marketplace.outbox.processor;

import com.marketplace.annotations.SpringIntegrationTest;
import com.marketplace.outbox.*;
import com.marketplace.outbox.processor.TestHandlerConfig.DummyPayload;
import com.marketplace.product.Product;
import com.marketplace.product.repository.ProductRepository;
import com.marketplace.util.MutableClock;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.convention.TestBean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringIntegrationTest
@Import(TestHandlerConfig.class)
public class OutboxEventProcessorTest {

    @Autowired
    private OutboxEventRepository repository;

    @Autowired
    private OutboxEventService service;

    @Autowired
    private ProductRepository dummyRepository;

    @Autowired
    private EntityManager entityManager;

    @TestBean
    Clock clock;

    static Clock clock() {
        return new MutableClock(
                Instant.parse("2026-01-01T00:00:00Z"),
                ZoneOffset.UTC
        );
    }

    static final String productNameThatShouldExist = "should exist";
    static final String productNameThatShouldNotExist = "should NOT exist";

    @AfterEach
    void cleanup() {
        repository.deleteAll();
        dummyRepository.deleteAll();
    }


    @Test
    void shouldPreserveOtherWorkWhenOneEventFailsWithRollback() {
        int eventsCount = 10;
        int exceptionEventIndex = eventsCount / 2;

        for (int i = 0; i < eventsCount; i++) {
            OutboxEvent event = i == exceptionEventIndex ?
                    new OutboxEventBuilder<DummyPayload>()
                            .withType(OutboxEventType.DUMMY2)
                            .withPayload(new DummyPayload(2L))
                            .build(clock)
                    : new OutboxEventBuilder<DummyPayload>()
                    .withType(OutboxEventType.DUMMY1)
                    .withPayload(new DummyPayload(1L))
                    .build(clock);
            repository.save(event);
        }

        // Initialization sanity checks
        List<Long> eventIds = repository.findPending(Instant.now(clock));
        assertThat(eventIds).hasSize(eventsCount);
        List<Product> persistedDummyEntities = dummyRepository.findAll();
        assertThat(persistedDummyEntities).isEmpty();

        service.processEvents();
        entityManager.clear();

        // Assert that only one event didn't complete
        List<OutboxEvent> persistedEvents = repository.findAll();
        assertThat(persistedEvents)
                .filteredOn(event -> event.getStatus() == OutboxEventStatus.PENDING)
                .hasSize(1);

        persistedDummyEntities = dummyRepository.findAll();

        // Assert that the failed event's work was rolled back
        assertThat(persistedDummyEntities).hasSize(eventsCount - 1);
        assertThat(persistedDummyEntities).
                filteredOn(product -> product.getName().equals(productNameThatShouldNotExist))
                .isEmpty();
    }

    @Test
    @DisplayName("Event should reschedule itself in the future when an exception is raised other than OutboxEventFailureException")
    void shouldRescheduleEventOnException() {
        OutboxEvent event = new OutboxEventBuilder<DummyPayload>()
                .withType(OutboxEventType.DUMMY2)
                .withPayload(new DummyPayload(1L))
                .build(clock);
        repository.save(event);

        // Initial sanity checks
        OutboxEvent persistedEvent = repository.findAll().getFirst();
        assertThat(persistedEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(persistedEvent.getAttempts()).isEqualTo(0);
        assertThat(persistedEvent.getProcessedAt()).isNull();

        service.processEvents();
        entityManager.clear();

        // Assert event is still pending and scheduled with a backoff
        persistedEvent = repository.findById(event.getId()).orElseThrow();
        assertThat(persistedEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(persistedEvent.getAttempts()).isEqualTo(1);
        assertThat(persistedEvent.getNextAttemptAt()).isAfter(Instant.now(clock));
        assertThat(persistedEvent.getProcessedAt()).isNull();

        service.processEvents();
        entityManager.clear();

        // Assert event does not get processed before its backoff is reached
        persistedEvent = repository.findById(event.getId()).orElseThrow();
        assertThat(persistedEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(persistedEvent.getAttempts()).isEqualTo(1);

        // Advance time to allow its processing
        ((MutableClock) clock).set(persistedEvent.getNextAttemptAt());
        service.processEvents();
        entityManager.clear();

        // Assert event gets processed again
        persistedEvent = repository.findById(event.getId()).orElseThrow();
        assertThat(persistedEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(persistedEvent.getAttempts()).isEqualTo(2);
        assertThat(persistedEvent.getProcessedAt()).isNull();
    }

    @Test
    void shouldTerminallyFailEventOnOutboxEventFailureException() {
        OutboxEvent event = new OutboxEventBuilder<DummyPayload>()
                .withType(OutboxEventType.DUMMY3)
                .withPayload(new DummyPayload(1L))
                .build(clock);
        repository.save(event);

        // Initial sanity checks
        OutboxEvent persistedEvent = repository.findAll().getFirst();
        assertThat(persistedEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(persistedEvent.getAttempts()).isEqualTo(0);
        assertThat(persistedEvent.getProcessedAt()).isNull();
        assertThat(dummyRepository.findAll()).isEmpty();

        service.processEvents();
        entityManager.clear();

        // Assert event terminally failed
        persistedEvent = repository.findAll().getFirst();
        assertThat(persistedEvent.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(persistedEvent.getAttempts()).isEqualTo(0);
        assertThat(persistedEvent.getProcessedAt()).isEqualTo(Instant.now(clock));

        // Assert event did NOT rollback its work
        assertThat(dummyRepository.findAll()).isNotEmpty();
    }
}
