package com.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Lombok generates methods to read and update the response fields.
@Getter
@Setter
// Lombok generates empty and full constructors for the DTO.
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {

    private long id;
    private String name;
}
