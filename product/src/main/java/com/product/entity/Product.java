package com.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

// Lombok creates methods for reading and updating the fields.
@Getter
@Setter
// Lombok creates constructors with all fields and with no arguments.
@AllArgsConstructor
@NoArgsConstructor
// Tell JPA that this class is stored as a database table.
@Entity
// Use the "products" table for Product records.
@Table(name = "products")
public class Product {

    // Mark this field as the table's primary key.
    @Id
    // Let the database generate a new ID for each product.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Map this field to a database column with a maximum length of 45.
    @Column(name = "name",length = 45)
    private String name;

    // Many products can belong to one subcategory.
    @ManyToOne
    // Store the subcategory relationship in the sub_category_id column.
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;

    // One product can have multiple brands; save and remove them with the product.
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Brand> brands = new LinkedHashSet<>();

}
