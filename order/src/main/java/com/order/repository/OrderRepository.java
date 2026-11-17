package com.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// JpaRepository supplies common database operations for orders.
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForPaymentStatusUpdate(@Param("id") Long id);
}
