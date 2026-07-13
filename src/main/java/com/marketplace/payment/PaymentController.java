package com.marketplace.payment;

import com.marketplace.payment.dto.PaymentInitiateResponse;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/reservations/{reservationId}/payment")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    PaymentInitiateResponse initiatePayment(@PositiveOrZero @PathVariable Long reservationId)  {
       return paymentService.initiatePayment(reservationId);
    }
}
