package com.marketplace.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateReservationRequest(
        @NotNull
        @Positive
        Integer quantity,

        @NotNull
        @PositiveOrZero
        Long productVersion
) {}
