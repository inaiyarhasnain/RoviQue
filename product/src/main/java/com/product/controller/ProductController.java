package com.product.controller;

import com.product.dto.ApiResponse;
import com.product.dto.CategoryDto;
import com.product.dto.ProductPriceResponse;
import com.product.dto.ProductDto;
import com.product.service.CatalogService;
import com.product.service.CategoryService;
import com.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Tell Spring this class receives HTTP requests and returns API responses.
@RestController
// Add this URL prefix to every endpoint in this controller.
@RequestMapping("/api/v1/product")
public class ProductController {
    private CategoryService categoryService;
    private ProductService productService;
    private CatalogService catalogService;

    public ProductController(
            CategoryService categoryService,
            ProductService productService,
            CatalogService catalogService) {
        this.categoryService = categoryService;
        this.productService = productService;
        this.catalogService = catalogService;
    }

    // Load categories and wrap them in a response for the caller.
    // Map GET requests for this path to the category-list method.
    @GetMapping("/list/categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategories(){
        List<CategoryDto> categoriesDto = categoryService.findAll();

        ApiResponse<List<CategoryDto>> response = new ApiResponse<>();

        if (categoriesDto!=null){
            response.setMessage("All categories data fetched!");
            response.setStatus(200);
            response.setData(categoriesDto);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        response.setMessage("No categories data found!");
        response.setStatus(500);
        response.setData(null);
        return new ResponseEntity<>(response,HttpStatus.INTERNAL_SERVER_ERROR);

    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<CategoryDto> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.findByCategoryId(id));
    }

    // Search products using the keyword sent in the URL.
    // Map GET requests for this path to the product-search method.
    @GetMapping("/list/search")
    public ResponseEntity<ApiResponse<List<ProductDto>>> searchProducts(
            // Read the keyword from the URL query string, for example ?keyword=shirt.
            @RequestParam String keyword
    ){
        List<ProductDto> productDtos = productService.searchProducts(keyword);
        ApiResponse<List<ProductDto>> response = new ApiResponse<>();
        if (productDtos!=null){
            response.setMessage("All adat fetched!");
            response.setStatus(200);
            response.setData(productDtos);
            return new ResponseEntity<>(response,HttpStatus.OK);
        }
        response.setMessage("data not found");
        response.setStatus(500);
        response.setData(null);
        return new ResponseEntity<>(response,HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDto> getProduct(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.findByProductId(id));
    }

    @GetMapping("/brands/{brandId}")
    public ResponseEntity<com.product.dto.BrandDto> getBrand(@PathVariable Integer brandId) {
        return ResponseEntity.ok(catalogService.findBrandById(brandId));
    }

    @GetMapping("/{productId}/brands/{brandId}/price")
    public ResponseEntity<ProductPriceResponse> getBrandPrice(
            @PathVariable Integer productId,
            @PathVariable Integer brandId) {
        return ResponseEntity.ok(productService.findBrandPrice(productId, brandId));
    }

}
