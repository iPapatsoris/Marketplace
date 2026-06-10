package com.marketplace.invetory.reservation;

import com.marketplace.invetory.reservation.dto.CreateInventoryReservationRequest;
import com.marketplace.invetory.reservation.dto.CreateInventoryReservationResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/products/{productID}/reservations")
public class InventoryReservationController {
    private final InventoryReservationService inventoryReservationService;

    @PostMapping
    public CreateInventoryReservationResponse reserveProduct(@PositiveOrZero @PathVariable Long productID,
                                                             @Valid @RequestBody CreateInventoryReservationRequest dto) {
        return inventoryReservationService.createInventoryReservation(productID, dto);
    }
}
