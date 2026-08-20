package com.marketplace.product.dto;

import java.math.BigDecimal;

public record CreateProductResponse(Long id, String name, BigDecimal price, int inventory) {
}
