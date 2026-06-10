package com.marketplace.invetory.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateInventoryReservationRequest(
        @NotNull
        @Positive
        Integer quantity
) {}
