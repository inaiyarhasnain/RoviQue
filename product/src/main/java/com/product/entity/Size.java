package com.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Lombok creates methods to read and update the fields.
@Getter
@Setter
// Lombok creates full and empty constructors.
@AllArgsConstructor
@NoArgsConstructor
// Tell JPA to store Size objects in the database.
@Entity
// Store size records in the "size" table.
@Table(name = "size")
public class Size {

    // Let JPA use this field as the primary key.
    @Id
    // Let the database generate the size ID when saving a row.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String size;
    private String quantity;

    // Many size records can belong to one brand.
    @ManyToOne
    // Store the related brand ID in the brand_id column.
    @JoinColumn(name = "brand_id")
    private Brand brand;

}
