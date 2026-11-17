package com.cart.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
// Lombok generates getter and setter methods for the fields.
@Getter
@Setter
// Lombok generates full and empty constructors.
@AllArgsConstructor
@NoArgsConstructor
// Tell JPA this class is stored in the database.
@Entity
// Save each cart item in the "cart_items" table.
@Table(name = "cart_items")
public class CartItem {

    // Mark this field as the primary key.
    @Id
    // Let the database assign an ID to each saved cart item.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private Long brandId;
    private Integer quantity;
    private BigDecimal price;

    // Many cart items can belong to one cart.
    @ManyToOne
    // Store the related cart's ID in the cart_id column.
    @JoinColumn(name = "cart_id")
    private Cart cart;
}
