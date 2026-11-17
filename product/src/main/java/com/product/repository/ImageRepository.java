package com.product.repository;

import com.product.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Integer> {

    // Spring Data builds a query from this method name to find images for a brand.
    List<Image> findAllByBrand_Id(Integer brandId);
}
