package com.marketplace.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank
        String name,

        @PositiveOrZero
        @NotNull
        BigDecimal price,

        @PositiveOrZero
        @NotNull
        int inventory
) {}
