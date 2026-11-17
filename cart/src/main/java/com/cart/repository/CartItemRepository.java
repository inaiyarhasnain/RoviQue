package com.cart.repository;

import com.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository supplies common database operations for cart items.
public interface CartItemRepository extends JpaRepository<CartItem,Long> {
}
