package com.cart.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Lombok creates methods to read and update the response fields.
@Getter
@Setter
// Lombok creates an empty constructor for JSON conversion.
@NoArgsConstructor
public class CartResponse {
    private String uuid;
    private Long userId;
    private List<CartItemResponse> items;
}
