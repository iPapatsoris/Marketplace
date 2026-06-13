package com.marketplace.product.repository;

import com.marketplace.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    @Query("""
            SELECT inventory
            FROM Product
            WHERE id = :id
            """)
    Optional<Integer> findInventoryById(Long id);

    boolean existsByIdAndVersion(Long id, Long version);
}