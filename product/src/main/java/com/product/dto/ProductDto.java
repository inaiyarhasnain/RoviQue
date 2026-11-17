package com.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

// Lombok generates getter and setter methods for the response fields.
@Getter
@Setter
// Lombok generates constructors with all fields and with no arguments.
@AllArgsConstructor
@NoArgsConstructor
public class ProductDto {
    private Integer id;
    private String name;
    private SubCategoryDto subCategory;
    private Set<BrandDto> brands = new LinkedHashSet<>();
}
