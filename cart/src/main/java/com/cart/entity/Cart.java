package com.cart.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

// Lombok creates methods to read and update the fields.
@Getter
@Setter
// Lombok creates full and empty constructors.
@AllArgsConstructor
@NoArgsConstructor
// Tell JPA this class is stored in the database.
@Entity
// Save carts in the "cart" database table.
@Table(name = "cart")
public class Cart {

    // Mark this field as the primary key.
    @Id
    // Let the database create a new ID when a cart is saved.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Require each cart ID to be different in the database.
    @Column(unique = true)
    private String uuid;

    private Long userId; // nullable

    // One cart can contain many items; save and remove them with the cart.
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();
}
