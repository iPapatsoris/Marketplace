package com.marketplace.reservation;

import com.marketplace.product.Product;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.marketplace.reservation.ReservationStatus.ACTIVE;

@Entity
@Table
@Data
public class Reservation {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, columnDefinition = "text")
    private String productSnapshot;

    @Column
    private Long orderId;

    public Reservation() {
        status = ACTIVE;
        expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES);
    }
}
