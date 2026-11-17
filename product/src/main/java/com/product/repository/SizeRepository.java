package com.product.repository;

import com.product.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository supplies common database operations for Size records.
public interface SizeRepository extends JpaRepository<Size, Integer> {
}
