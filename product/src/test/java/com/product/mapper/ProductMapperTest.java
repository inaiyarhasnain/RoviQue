package com.product.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.product.entity.Brand;
import com.product.entity.Category;
import com.product.entity.Image;
import com.product.entity.Product;
import com.product.entity.Size;
import com.product.entity.SubCategory;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProductMapperTest {

    // Tell JUnit to run this mapper-conversion check as a test.
    @Test
    void convertsBidirectionalProductGraphToFiniteJson() throws Exception {
        // Build linked entities like the relationships that may come from the database.
        Category category = new Category();
        category.setId(1L);
        category.setName("Fruit");

        SubCategory subCategory = new SubCategory();
        subCategory.setId(2);
        subCategory.setName("Fresh fruit");
        subCategory.setCategory(category);

        Product product = new Product();
        product.setId(3);
        product.setName("Apple");
        product.setSubCategory(subCategory);
        subCategory.setProducts(new LinkedHashSet<>(Set.of(product)));
        category.setSubCategories(new LinkedHashSet<>(Set.of(subCategory)));

        Brand brand = new Brand();
        brand.setId(4);
        brand.setName("Orchard");
        brand.setProduct(product);
        product.setBrands(new LinkedHashSet<>(Set.of(brand)));

        Size size = new Size();
        size.setId(5);
        size.setSize("Large");
        size.setQuantity("10");
        size.setBrand(brand);
        brand.setSizes(new LinkedHashSet<>(Set.of(size)));

        Image image = new Image();
        image.setId(6);
        image.setUrl("https://rovique.s3.eu-north-1.amazonaws.com/brands/4/apple.jpg");
        image.setBrand(brand);
        brand.setImages(new LinkedHashSet<>(Set.of(image)));

        // Convert the product to its API DTO and then to JSON.
        String json = new ObjectMapper().writeValueAsString(
                ProductMapper.convertProductToProductDto(
                        product, url -> "https://signed.example.test/image"));

        // Check the expected response values and ensure entity links were not serialized.
        assertEquals("Apple", new ObjectMapper().readTree(json).get("name").asText());
        assertEquals(
                "https://signed.example.test/image",
                new ObjectMapper().readTree(json).get("brands").get(0).get("images").get(0).get("url").asText());
        assertFalse(json.contains("\"products\""));
        assertFalse(json.contains("\"product\""));
        assertFalse(json.contains("\"brand\""));
    }
}
