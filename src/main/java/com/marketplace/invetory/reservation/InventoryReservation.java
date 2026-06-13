package com.marketplace.invetory.reservation;

import com.marketplace.product.Product;
import com.marketplace.product.ProductSnapshot;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private ProductSnapshot productSnapshot;

    public InventoryReservation() {
        status = ACTIVE;
        expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES);
    }
}
