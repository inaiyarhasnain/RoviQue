package com.payment.dto;

// Carries the payment status that the payment service verified with Stripe.
public record PaymentStatusUpdateRequest(String paymentStatus) {
}
