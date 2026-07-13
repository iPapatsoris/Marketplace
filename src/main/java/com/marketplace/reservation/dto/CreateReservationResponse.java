package com.marketplace.reservation.dto;

import java.time.Instant;

public record CreateReservationResponse(
        Long reservationID,
        Integer quantity,
        Instant expiresAt
) {}
