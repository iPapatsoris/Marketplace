package com.marketplace.reservation;

import com.marketplace.reservation.entity.Reservation;
import com.marketplace.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @NativeQuery("""
        UPDATE reservation 
        SET status = 'EXPIRED' 
        WHERE status = 'ACTIVE' AND expires_at <= :now
        RETURNING *
""")
    List<Reservation> expireReservations(Instant now);

    @Modifying
    @Query("""
        UPDATE Reservation 
        SET status = 'PAYMENT_INITIATED' 
        WHERE id = :id
            AND status = 'ACTIVE' 
            AND expiresAt > :now
""")
    int markAsPaymentInitiated(Long id, Instant now);

    @Modifying
    @Query("""
        UPDATE Reservation 
        SET status = 'PAID', 
            orderId = :orderId
        WHERE id = :id
            AND status = 'PAYMENT_INITIATED' 
""")
    int markAsPaid(Long id, Long orderId);

    @Modifying
    @Query("""
        UPDATE Reservation 
        SET status = 'ACTIVE'
        WHERE id = :id
            AND status = 'PAYMENT_INITIATED'
""")
    int markAsActive(Long id);

    @Query("""
        SELECT status
        FROM Reservation
        WHERE id = :id
""")
    Optional<ReservationStatus> findStatusById(Long id);

    @Query("""
        SELECT productSnapshot
        FROM Reservation 
        WHERE id = :id
""")
    Optional<String> findProductSnapshotById(Long id);
}
