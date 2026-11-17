package com.cart.repository;

import com.cart.entity.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// JpaRepository supplies common database operations for carts.
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Spring Data builds a query to find a cart by its UUID.
    Optional<Cart> findByUuid(String uuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cart from Cart cart where cart.uuid = :uuid")
    Optional<Cart> findByUuidForUpdate(@Param("uuid") String uuid);
}
