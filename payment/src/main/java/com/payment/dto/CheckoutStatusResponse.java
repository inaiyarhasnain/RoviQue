package com.payment.dto;

// Holds the payment result and a readable message for the caller.
public record CheckoutStatusResponse(
        Long orderId,
        boolean paid,
        String message,
        String paymentStatus) {
}
