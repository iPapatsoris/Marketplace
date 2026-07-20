package com.marketplace.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record UpdateProductRequest(
        Long version,

        @NotBlank
        String name,

        @PositiveOrZero
        BigDecimal price,

        @PositiveOrZero
        Integer inventory
){};

