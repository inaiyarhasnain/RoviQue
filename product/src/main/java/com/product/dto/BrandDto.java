package com.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

// Lombok generates getter and setter methods for the response fields.
@Getter
@Setter
// Lombok generates a constructor that accepts all fields.
@AllArgsConstructor
// Lombok generates an empty constructor.
@NoArgsConstructor
public class BrandDto {
    private Integer id;
    private String name;
    private BigDecimal price;
    private Set<SizeDto> sizes = new LinkedHashSet<>();
    private Set<ImageDto> images = new LinkedHashSet<>();
}
