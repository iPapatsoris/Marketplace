package com.marketplace.product.repository;

import com.marketplace.product.ProductSnapshot;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Custom implementation as JPA / Hibernate doesn't support RETURNING
 */
@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl
        implements ProductRepositoryCustom {

    private final EntityManager em;

    public Optional<ProductSnapshot> reserveInventory(
            Long id,
            Long version,
            int reservation) {

        List<Object[]> rows = em.createNativeQuery("""
            UPDATE product
            SET inventory = inventory - :reservation,
                version = version + 1
            WHERE id = :id
              AND version = :version
              AND inventory >= :reservation
            RETURNING name, price
            """)
                .setParameter("id", id)
                .setParameter("version", version)
                .setParameter("reservation", reservation)
                .getResultList();

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        Object[] row = rows.get(0);

        return Optional.of(
                new ProductSnapshot(
                        (String) row[0],
                        (BigDecimal) row[1]
                )
        );
    }
}
