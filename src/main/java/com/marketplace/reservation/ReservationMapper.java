package com.marketplace.reservation;

import com.marketplace.reservation.dto.CreateReservationRequest;
import com.marketplace.reservation.dto.CreateReservationResponse;
import com.marketplace.reservation.dto.ReservationResponse;
import com.marketplace.reservation.entity.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    Reservation updateEntity(CreateReservationRequest dto, @MappingTarget Reservation reservation);

    @Mapping(source = "id", target = "reservationID")
    CreateReservationResponse toCreateReservationResponse(Reservation entity);

    @Mapping(source = "id", target = "reservationId")
    ReservationResponse toReservationResponse(Reservation entity);
}

