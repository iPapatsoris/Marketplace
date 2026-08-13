package com.marketplace.outbox;

import com.marketplace.annotations.RepositoryTest;
import com.marketplace.outbox.OutboxEventProcessorTest.DummyPayload;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static com.marketplace.util.ConcurrencyControl.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

@RepositoryTest
public class OutboxEventRepositoryTest {

    @Autowired
    OutboxEventRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    TransactionTemplate tx;

    @MockitoBean
    private Clock clock;

    /** We are disabling @DataJpaTest's behavior of wrapping each test with a transaction
     *  in order to test concurrent transactions, so we need to manually undo DB changes after each test.
     */
    @AfterEach
    void cleanup() {
        repository.deleteAllInBatch();
    }

    @Nested
    class FindAndLockSingleEventSkipLockedTest {

        private void runThreads(Consumer<TransactionStatus> transaction1,
                                Consumer<TransactionStatus> transaction2)
                throws ExecutionException, InterruptedException {

            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            try {
                Future<?> t1 = executor.submit(() -> tx.executeWithoutResult(transaction1));
                Future<?> t2 = executor.submit(() -> tx.executeWithoutResult(transaction2));

                t1.get();
                t2.get();
            } finally {
                executor.shutdownNow();
            }
        }

        private void assertEqualOutboxEvent(OutboxEvent actual, OutboxEvent expected) {
            assertThat(actual)
                    .usingRecursiveComparison()
                    .withComparatorForType(
                            Comparator.comparing(i -> i.truncatedTo(ChronoUnit.MILLIS)),
                            Instant.class)
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("If the event is locked by a transaction, others should NOT block when attempting to acquire it, should skip it instead")
        @Transactional(propagation = NOT_SUPPORTED)
        void shouldSkipEventIfLocked() throws ExecutionException, InterruptedException {
            OutboxEvent outboxEvent = new OutboxEventBuilder<DummyPayload>().build(clock);
            Long id = tx.execute(status -> (Long) entityManager.persistAndGetId(outboxEvent));

            CountDownLatch rowLocked = new CountDownLatch(1);
            CountDownLatch secondQueryFinished = new CountDownLatch(1);

            Consumer<TransactionStatus> transaction1 = (TransactionStatus status) -> {
                Optional<OutboxEvent> result = repository.findAndLockSingleEventSkipLocked(id);
                assertThat(result).isPresent();
                assertEqualOutboxEvent(result.get(), outboxEvent);

                rowLocked.countDown();
                await(secondQueryFinished);
            };

            Consumer<TransactionStatus> transaction2 = (TransactionStatus status) -> {
                await(rowLocked);

                Optional<OutboxEvent> result = repository.findAndLockSingleEventSkipLocked(id);
                assertThat(result).isEmpty();

                secondQueryFinished.countDown();
            };

            runThreads(transaction1, transaction2);
        }

        @Test
        @DisplayName("Locking an event should not interfere with locking other events")
        @Transactional(propagation = NOT_SUPPORTED)
        void shouldLockDifferentEvents() throws ExecutionException, InterruptedException {
            OutboxEvent outboxEvent1 = new OutboxEventBuilder<DummyPayload>().build(clock);
            OutboxEvent outboxEvent2 = new OutboxEventBuilder<DummyPayload>().build(clock);

            record InsertedIds(Long id1, Long id2) {}

            InsertedIds ids = tx.execute(status -> {
                Long id1 = (Long) entityManager.persistAndGetId(outboxEvent1);
                Long id2 = (Long) entityManager.persistAndGetId(outboxEvent2);
                return new InsertedIds(id1, id2);
            });

            Long id1 = ids.id1;
            Long id2 = ids.id2;

            CountDownLatch row1Locked = new CountDownLatch(1);
            CountDownLatch secondQueryFinished = new CountDownLatch(1);

            Consumer<TransactionStatus> transaction1 = (TransactionStatus status) -> {
                Optional<OutboxEvent> result = repository.findAndLockSingleEventSkipLocked(id1);
                assertThat(result).isPresent();
                assertEqualOutboxEvent(result.get(), outboxEvent1);

                row1Locked.countDown();
                await(secondQueryFinished);
            };

            Consumer<TransactionStatus> transaction2 = (TransactionStatus status) -> {
                await(row1Locked);

                Optional<OutboxEvent> result = repository.findAndLockSingleEventSkipLocked(id2);
                assertThat(result).isPresent();
                assertEqualOutboxEvent(result.get(), outboxEvent2);

                secondQueryFinished.countDown();
            };

            runThreads(transaction1, transaction2);
        }
    }
}
