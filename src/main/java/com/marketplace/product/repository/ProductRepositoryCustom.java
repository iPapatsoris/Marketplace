package com.marketplace.product.repository;

import com.marketplace.product.ProductSnapshot;

import java.util.Optional;

public interface ProductRepositoryCustom {

    Optional<ProductSnapshot> reserveInventory(
            Long id,
            Long version,
            int reservation);
}
