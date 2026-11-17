package com.payment.dto;

public record StripeWebhookResponse(String status, String message) {
}
