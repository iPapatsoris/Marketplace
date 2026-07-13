package com.marketplace.reservation.exception;

public class ReservationExpiredException extends RuntimeException {
    public ReservationExpiredException(Long id) {
        super("Reservation with id %d has expired".formatted(id));
    }
}
