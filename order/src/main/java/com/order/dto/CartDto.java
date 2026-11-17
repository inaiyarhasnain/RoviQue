package com.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Lombok creates methods to read and update the cart data received from the cart service.
@Getter
@Setter
// Lombok creates an empty constructor so JSON can be converted into this DTO.
@NoArgsConstructor
public class CartDto {
    private String uuid;
    private Long userId;
    private List<CartItemDto> items;
}
