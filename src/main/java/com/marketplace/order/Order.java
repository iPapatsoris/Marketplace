package com.marketplace.order;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "purchase_order")
@Data
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String productSnapshot;

    @Column(nullable = false)
    private Long reservationId;

    public Order(String productSnapshot, Long reservationId) {
        this.productSnapshot = productSnapshot;
        this.reservationId = reservationId;
    }
}
