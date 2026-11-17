package com.payment.dto;

import java.math.BigDecimal;

// Holds a product line copied into the order by the order service.
public record OrderItemPaymentDetails(
        Long productId,
        Long brandId,
        Integer quantity,
        BigDecimal price,
        BigDecimal lineTotal) {
}
