package com.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Lombok generates methods to read and update the response fields.
@Getter
@Setter
// Create full and empty constructors automatically.
@AllArgsConstructor
@NoArgsConstructor
public class ImageDto {
    private Integer id;
    private String url;
}
