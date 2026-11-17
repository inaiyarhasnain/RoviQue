package com.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Lombok generates getter and setter methods for each field.
@Getter
@Setter
// Lombok generates constructors with all fields and with no arguments.
@AllArgsConstructor
@NoArgsConstructor
// Tell JPA to store Image objects in the database.
@Entity
// Save image records in the "images" table.
@Table(name = "images")
public class Image {
    // Let JPA use this field as the primary key.
    @Id
    // Let the database create a new image ID when saving a row.
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    private Integer id;

    // Map the image URL to a database column that can hold up to 2000 characters.
    @Column(name = "url",length = 2000)
    private String url;

    // Many images can belong to one brand.
    @ManyToOne
    // Store the related brand ID in the brand_id column.
    @JoinColumn(name = "brand_id")
    private Brand brand;

}
