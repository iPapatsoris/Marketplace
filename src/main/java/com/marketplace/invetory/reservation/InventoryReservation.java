package com.marketplace.invetory.reservation;

import com.marketplace.product.Product;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.marketplace.invetory.reservation.InventoryReservationStatus.ACTIVE;

@Entity
@Table
@Data
public class InventoryReservation {
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
    private InventoryReservationStatus status;

    @Column(nullable = false)
    private Instant expiresAt;

    public InventoryReservation() {
        status = ACTIVE;
        expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES);
    }
}
