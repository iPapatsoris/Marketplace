package com.marketplace.invetory.reservation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

public record CreateInventoryReservationResponse(
        Long reservationID,
        Integer quantity,
        Instant expiresAt
) {}
