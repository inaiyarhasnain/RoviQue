package com.payment.dto;

import java.math.BigDecimal;
import java.util.List;

// Holds the order, cart, and item details received from the order service.
public record OrderPaymentDetails(
        Long id,
        String cartUuid,
        BigDecimal totalAmount,
        String paymentStatus,
        List<OrderItemPaymentDetails> items) {
}
