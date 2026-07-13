package com.marketplace.reservation;

import com.marketplace.reservation.dto.CreateReservationRequest;
import com.marketplace.reservation.dto.CreateReservationResponse;
import com.marketplace.reservation.dto.ReservationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    Reservation toEntity(CreateReservationRequest dto);

    @Mapping(source = "id", target = "reservationID")
    CreateReservationResponse toCreateReservationResponse(Reservation entity);

    @Mapping(source = "id", target = "reservationId")
    ReservationResponse toReservationResponse(Reservation entity);
}

