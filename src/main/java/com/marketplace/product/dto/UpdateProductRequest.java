package com.marketplace.product.dto;

import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

// Any way to reuse the validation rules, to ensure they'll be the same
// between create and edit?

public record UpdateProductRequest(
        Long version,

        @NotBlank
        String name,

        @PositiveOrZero
        @NotNull
        BigDecimal price,

        @PositiveOrZero
        @NotNull
        int inventory
){};

