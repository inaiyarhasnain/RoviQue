package com.product.repository;

import com.product.entity.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;

// JpaRepository supplies common database operations for SubCategory records.
public interface SubCategoryRepository extends JpaRepository<SubCategory, Integer> {
}
