package com.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

// Lombok generates getter and setter methods.
@Getter
@Setter
// Lombok generates a full constructor and an empty constructor.
@AllArgsConstructor
@NoArgsConstructor
// Tell JPA to save subcategories as database rows.
@Entity
// Store subcategory rows in the "sub_categories" table.
@Table(name = "sub_categories")
public class SubCategory {
    // Mark this field as the primary key.
    @Id
    // Let the database assign an ID to each saved subcategory.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Map the name to a database column with a maximum length of 45.
    @Column(name = "name",length = 45)
    private String name;

    // Many subcategories can belong to one category.
    @ManyToOne
    // Store the related category ID in the category_id column.
    @JoinColumn(name = "category_id")
    private Category category;

    // A subcategory can contain multiple products; save and remove them with it.
    @OneToMany(mappedBy = "subCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Product> products = new LinkedHashSet<>();

}
