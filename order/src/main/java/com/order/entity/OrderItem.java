package com.order.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Tell JPA this class represents a database table.
@Entity
// Save order items in the "order_items" table.
@Table(name = "order_items")
// Lombok creates methods for reading and updating the fields.
@Getter
@Setter
// Lombok creates the empty constructor JPA needs.
@NoArgsConstructor
public class OrderItem {
    // Mark this field as the primary key.
    @Id
    // Let the database assign an ID to each saved order item.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private Long brandId;
    private Integer quantity;

    private BigDecimal price;

    // Load the parent order only when the code needs it, and require an order.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // Store the parent order ID in a required order_id column.
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}
