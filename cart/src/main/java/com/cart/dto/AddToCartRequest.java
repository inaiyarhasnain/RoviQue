package com.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
// Lombok creates methods to read and update the request fields.
@Getter
@Setter
// Lombok creates full and empty constructors for the request object.
@AllArgsConstructor
@NoArgsConstructor
public class AddToCartRequest {
    @NotNull
    @Positive
    @Max(Integer.MAX_VALUE)
    private Long productId;

    @NotNull
    @Positive
    @Max(Integer.MAX_VALUE)
    private Long brandId;

    @NotNull
    @Positive
    private Integer quantity;

    // Retained for older clients; checkout pricing is loaded from the product service.
    private BigDecimal price;
}
