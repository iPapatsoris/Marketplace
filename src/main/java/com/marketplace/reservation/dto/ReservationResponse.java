package com.marketplace.reservation.dto;

import com.marketplace.reservation.entity.ReservationStatus;

public record ReservationResponse(Long reservationId, ReservationStatus status, Long orderId) {
}
