package com.order.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Tell JPA this class represents a database table.
@Entity
// Save order rows in the "orders" table.
@Table(name = "orders")
// Lombok creates methods to read and update the order fields.
@Getter
@Setter
// Lombok creates an empty constructor required by JPA.
@NoArgsConstructor
public class Order {
    // Mark this field as the primary key.
    @Id
    // Let the database create an ID for each new order.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Require every saved order to have a cart ID.
    @Column(nullable = false)
    private String cartUuid;

    private Long userId;

    // Require a total amount and store it with two digits after the decimal point.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // Require every saved order to have a status.
    @Column(nullable = false)
    private String status;

    // Keep payment progress separate from the order's fulfillment status.
    @Column(nullable = false, columnDefinition = "varchar(32) not null default 'PENDING'")
    private String paymentStatus = "PENDING";

    // Require every saved order to have a creation date and time.
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // One order can have many items; save and remove them with the order.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}
