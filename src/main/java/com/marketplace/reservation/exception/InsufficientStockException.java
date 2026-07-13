package com.marketplace.reservation.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productID, int reservationQuantity, Integer actualStock) {
        super(
                actualStock == null ? "Cannot reserve product with id %d: requested quantity %d exceeds available".formatted(productID, reservationQuantity) :
                "Cannot reserve product with id %d: requested quantity %d of %d available".formatted(productID, reservationQuantity, actualStock));
    }
}
