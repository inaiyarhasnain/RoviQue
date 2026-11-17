package com.payment.dto;

import java.math.BigDecimal;

// Holds the checkout result that the API sends back to its caller.
public record CheckoutSessionResponse(
        String message,
        String sessionId,
        String paymentUrl,
        BigDecimal amount,
        String currency) {
}
