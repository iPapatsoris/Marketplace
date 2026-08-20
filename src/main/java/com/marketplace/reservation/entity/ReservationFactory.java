package com.marketplace.reservation.entity;

import com.marketplace.product.Product;
import com.marketplace.reservation.ReservationMapper;
import com.marketplace.reservation.dto.CreateReservationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class ReservationFactory {

    private final Clock clock;
    private final ReservationMapper mapper;

    public Reservation create(CreateReservationRequest dto, String productSnapshot, Product product) {

        Reservation reservation = new Reservation(
                Instant.now(clock).plus(10, ChronoUnit.MINUTES),
                productSnapshot,
                product);
        mapper.updateEntity(dto, reservation);

        return reservation;
    }
}
