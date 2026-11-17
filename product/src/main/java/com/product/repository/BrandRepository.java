package com.product.repository;

import com.product.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository supplies common database operations for Brand records.
public interface BrandRepository extends JpaRepository<Brand, Integer> {
}
