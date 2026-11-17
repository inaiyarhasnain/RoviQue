package com.product.repository;

import com.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// JpaRepository supplies common database operations such as save, findById, and findAll.
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCase(String name);
}
