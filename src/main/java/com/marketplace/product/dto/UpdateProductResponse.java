package com.marketplace.product.dto;

import java.math.BigDecimal;

public record UpdateProductResponse(Long version, String name, BigDecimal price, int inventory) {
}
