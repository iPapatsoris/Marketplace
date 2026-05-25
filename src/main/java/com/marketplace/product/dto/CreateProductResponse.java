package com.marketplace.product.dto;

import java.math.BigDecimal;

public record CreateProductResponse(BigDecimal id, String name, BigDecimal price, int inventory) {
}
