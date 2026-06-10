package com.marketplace.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying
    @Query("""
            UPDATE Product
            SET inventory = inventory - :reservation
            WHERE id = :id AND inventory >= :reservation
            """)
    int reserveInventory(Long id, int reservation);

    @Query("""
            SELECT inventory
            FROM Product
            WHERE id = :id
            """)
    Optional<Integer> findInventoryByID(Long id);
}