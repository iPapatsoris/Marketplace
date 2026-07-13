package com.marketplace.product;

import com.marketplace.reservation.Reservation;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    @OneToMany(mappedBy = "product", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    List<Reservation> reservations;
}


