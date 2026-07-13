package com.marketplace.reservation;

import com.marketplace.payment.PaymentService;
import com.marketplace.payment.dto.PaymentInitiateResponse;
import com.marketplace.reservation.dto.CreateReservationRequest;
import com.marketplace.reservation.dto.CreateReservationResponse;
import com.marketplace.reservation.dto.ReservationResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping
public class ReservationController {
    private final ReservationService reservationService;
    private final PaymentService paymentService;

    @PostMapping("/products/{productId}/reservations")
    public CreateReservationResponse reserveProduct(@PositiveOrZero @PathVariable Long productId,
                                                    @Valid @RequestBody CreateReservationRequest dto) {
        return reservationService.createReservation(productId, dto);
    }

    @GetMapping("/reservations/{reservationId}")
    public ReservationResponse getReservation(@PositiveOrZero @PathVariable Long reservationId) {
        return reservationService.getReservation(reservationId);
    }

    @PostMapping("/reservations/{reservationId}/payment")
    PaymentInitiateResponse initiatePayment(@PositiveOrZero @PathVariable Long reservationId)  {
        return paymentService.initiatePayment(reservationId);
    }
}
