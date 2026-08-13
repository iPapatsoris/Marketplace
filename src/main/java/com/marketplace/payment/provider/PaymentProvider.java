package com.marketplace.payment.provider;

import org.springframework.stereotype.Component;

@Component
public class PaymentProvider {
    public PaymentResult complete() {
        return PaymentResult.SUCCESS;
    }
}
