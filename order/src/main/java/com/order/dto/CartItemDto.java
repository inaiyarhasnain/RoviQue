package com.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Lombok creates methods to read and update cart-item data from the cart service.
@Getter
@Setter
// Lombok creates an empty constructor so JSON can be converted into this DTO.
@NoArgsConstructor
public class CartItemDto {
    private Long productId;
    private Long brandId;
    private Integer quantity;
    private BigDecimal price;
}
