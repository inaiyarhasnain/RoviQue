package com.payment.dto;

// Holds the order ID and the URLs Stripe should redirect to after checkout.
public record CreateCheckoutSessionRequest(
        Long orderId,
        String successUrl,
        String cancelUrl) {
}
