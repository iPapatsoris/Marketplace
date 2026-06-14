package com.marketplace.invetory.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    @Modifying
    @Query("""
        UPDATE InventoryReservation 
        SET status = 'EXPIRED' 
        WHERE status = 'ACTIVE' AND expiresAt <= :now
""")
    int expireReservations(Instant now);
}
