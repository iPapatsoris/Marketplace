package com.marketplace.invetory.reservation.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productID, int reservationQuantity, int actualStock) {
        super("Cannot reserve product with id %d: requested quantity %d of %d available".formatted(productID, reservationQuantity, actualStock));
    }
}
