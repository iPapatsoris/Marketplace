package com.marketplace.product.dto;

import java.math.BigDecimal;

public record UpdateProductResponse(String name, BigDecimal price, int inventory) {
}
