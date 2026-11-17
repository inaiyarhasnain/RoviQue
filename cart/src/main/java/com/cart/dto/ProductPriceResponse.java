package com.cart.dto;

import java.math.BigDecimal;

public record ProductPriceResponse(Long productId, Long brandId, BigDecimal price) {
}
