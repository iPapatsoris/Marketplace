package com.marketplace.product;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table
@Data
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue
    private Long id;

    @Version
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

}


