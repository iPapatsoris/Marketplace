package com.marketplace.product;

import com.marketplace.invetory.reservation.InventoryReservation;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Entity
@Table
@Data
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue
    private Long id;

    @Version
    @Column(nullable = false)
    Long version;

    @Column(nullable = false)
    String name;

    @Column(nullable = false)
    BigDecimal price;

    @Column(nullable = false)
    int inventory;

    public Product(String name, BigDecimal price, int inventory) {
       this.name = name;
       this.price = price;
       this.inventory = inventory;
    }

    @OneToMany(mappedBy = "product", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    List<InventoryReservation> reservations;
}


