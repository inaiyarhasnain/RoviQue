package com.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

// Lombok generates methods to read and update the fields.
@Getter
@Setter
// Lombok generates constructors with all fields and with no arguments.
@AllArgsConstructor
@NoArgsConstructor
// Tell JPA that Category objects are stored in the database.
@Entity
// Use the "categories" table for category rows.
@Table(name = "categories")
public class Category {
    // Mark this field as the primary key.
    @Id
    // Let the database generate the ID when a category is saved.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Map the category name to a database column with a maximum length of 45.
    @Column(name = "name",length = 45)
    private String name;

    // A category can contain many subcategories; save and remove them with it.
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SubCategory> subCategories = new LinkedHashSet<>();

}
