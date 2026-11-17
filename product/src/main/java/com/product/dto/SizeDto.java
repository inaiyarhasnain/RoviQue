package com.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Lombok generates getter and setter methods for the response fields.
@Getter
@Setter
// Create full and empty constructors automatically.
@AllArgsConstructor
@NoArgsConstructor
public class SizeDto {
    private Integer id;
    private String size;
    private String quantity;
}
