package com.marketplace.invetory.reservation;

import com.marketplace.invetory.reservation.dto.CreateInventoryReservationRequest;
import com.marketplace.invetory.reservation.dto.CreateInventoryReservationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryReservationMapper {
    InventoryReservation toEntity(CreateInventoryReservationRequest dto);

    @Mapping(source = "id", target = "reservationID")
    CreateInventoryReservationResponse toCreateInventoryReservationResponse(InventoryReservation entity);
}

