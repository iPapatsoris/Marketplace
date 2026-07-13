package com.marketplace.reservation.exception;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(Long id) {
        super("Reservation with id %d does not exist".formatted(id));
    }
}
