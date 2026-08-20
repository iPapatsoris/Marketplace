package com.marketplace.reservation.entity;

import com.marketplace.product.Product;
import com.marketplace.product.ProductBuilder;

import java.time.Instant;

import static com.marketplace.reservation.entity.ReservationStatus.ACTIVE;

public class ReservationBuilder {
    private Long id = null;
    private Product product = ProductBuilder.aProduct().build();
    private int quantity = 5;
    private ReservationStatus status = ACTIVE;
    private Instant expiresAt =
            Instant.parse("2026-01-01T12:00:00Z");
    private String productSnapshot = "{}";
    private Long orderId = null;

    public ReservationBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public ReservationBuilder withQuantity(int quantity) {
        this.quantity = quantity;
        return this;
    }

    public ReservationBuilder withStatus(ReservationStatus status) {
        this.status = status;
        return this;
    }

    public ReservationBuilder withOrderId(Long orderId) {
        this.orderId = orderId;
        return this;
    }

    public ReservationBuilder withProduct(Product product) {
        this.product = product;
        return this;
    }

    public ReservationBuilder withProductSnapshot(String productSnapshot) {
        this.productSnapshot = productSnapshot;
        return this;
    }

    public Reservation build() {
        Reservation reservation =
                new Reservation(
                        expiresAt,
                        productSnapshot,
                        product);

        reservation.setId(id);
        reservation.setQuantity(quantity);
            reservation.setStatus(status);
        reservation.setOrderId(orderId);

        return reservation;
    }
}
