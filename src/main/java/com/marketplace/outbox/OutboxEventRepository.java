package com.marketplace.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    @Query("""
        SELECT id
        FROM OutboxEvent 
        WHERE status = 'PENDING' AND :now >= nextAttemptAt
""")
    List<Long> findPending(
            Instant now
    );

    @NativeQuery("""
        SELECT *
        FROM outbox_event
        WHERE id = :id
        FOR UPDATE SKIP LOCKED
""")
    Optional<OutboxEvent> findAndLockSingleEventSkipLocked(Long id);
}


