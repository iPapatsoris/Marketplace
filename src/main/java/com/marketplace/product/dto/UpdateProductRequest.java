package com.marketplace.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotNull
        Long version,

        // TODO: change to allowing NULL, but with custom rules if not NULL (length, whitespace, etc)
        @NotBlank
        String name,

        @PositiveOrZero
        BigDecimal price,

        @PositiveOrZero
        Integer inventory
){};

