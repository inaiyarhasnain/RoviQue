package com.product.repository;

import com.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    // Tell Spring Data to run this custom JPQL search instead of deriving a query from the method name.
    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN p.brands b
        LEFT JOIN p.subCategory sc
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(sc.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    // Bind this method argument to the :keyword placeholder in the query.
    List<Product> searchProducts(@Param("keyword") String keyword);
}
