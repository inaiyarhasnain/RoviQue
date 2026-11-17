package com.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

// Lombok creates getter and setter methods for the fields.
@Getter
@Setter
// Lombok creates full and empty constructors.
@AllArgsConstructor
@NoArgsConstructor
// Tell JPA to save Brand objects in the database.
@Entity
// Store brand rows in the "brands" table.
@Table(name = "brands")
public class Brand {
    // Use this field as the database primary key.
    @Id
    // Let the database assign the brand ID when a row is inserted.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private BigDecimal price;

    // Many brands can belong to one product.
    @ManyToOne
    // Store the related product ID in the product_id column.
    @JoinColumn(name = "product_id")
    private Product product;

    // A brand can have multiple sizes; save and delete them with the brand.
    @OneToMany(mappedBy = "brand", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Size> sizes = new LinkedHashSet<>();

    // A brand can have multiple images; save and delete them with the brand.
    @OneToMany(mappedBy = "brand", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Image> images = new LinkedHashSet<>();

}
