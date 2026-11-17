package com.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Lombok generates getter and setter methods for the response fields.
@Getter
@Setter
// Lombok creates empty and full constructors for this response object.
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private Long productId;
    private Long brandId;
    private Integer quantity;
    private BigDecimal price;
}
