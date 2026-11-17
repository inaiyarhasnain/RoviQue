package com.product.service;

import com.product.dto.BrandCreateRequest;
import com.product.dto.BrandDto;
import com.product.dto.BrandUpdateRequest;
import com.product.dto.CategoryDto;
import com.product.dto.CategoryUpsertRequest;
import com.product.dto.ProductDto;
import com.product.dto.ProductUpsertRequest;
import com.product.entity.Brand;
import com.product.entity.Category;
import com.product.entity.Product;
import com.product.entity.SubCategory;
import com.product.mapper.CategoryMapper;
import com.product.mapper.ProductMapper;
import com.product.repository.BrandRepository;
import com.product.repository.CategoryRepository;
import com.product.repository.ProductRepository;
import com.product.repository.SubCategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;

@Service
public class CatalogService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final ImageService imageService;

    public CatalogService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            SubCategoryRepository subCategoryRepository,
            ImageService imageService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.imageService = imageService;
    }

    @Transactional(readOnly = true)
    public BrandDto findBrandById(Integer id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));
        return toBrandDto(brand);
    }

    @Transactional
    public ProductDto createProduct(ProductUpsertRequest request) {
        Product product = new Product();
        product.setName(request.name().trim());
        product.setSubCategory(findSubCategory(request.subCategoryId()));
        return toProductDto(productRepository.save(product));
    }

    @Transactional
    public ProductDto updateProduct(Integer id, ProductUpsertRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        product.setName(request.name().trim());
        product.setSubCategory(findSubCategory(request.subCategoryId()));
        return toProductDto(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        for (Brand brand : new ArrayList<>(product.getBrands())) {
            imageService.deleteBrandFiles(brand.getId());
        }
        productRepository.delete(product);
    }

    @Transactional
    public CategoryDto createCategory(CategoryUpsertRequest request) {
        Category category = new Category();
        category.setName(request.name().trim());
        return CategoryMapper.convertCategoryToCategoryDto(categoryRepository.save(category));
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryUpsertRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        category.setName(request.name().trim());
        return CategoryMapper.convertCategoryToCategoryDto(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        if (!category.getSubCategories().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Category cannot be deleted while it has subcategories");
        }
        categoryRepository.delete(category);
    }

    @Transactional
    public BrandDto createBrand(BrandCreateRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        Brand brand = new Brand();
        brand.setName(request.name().trim());
        brand.setPrice(request.price());
        brand.setProduct(product);
        Brand savedBrand = brandRepository.save(brand);
        product.getBrands().add(savedBrand);
        return toBrandDto(savedBrand);
    }

    @Transactional
    public BrandDto updateBrand(Integer id, BrandUpdateRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));
        brand.setName(request.name().trim());
        brand.setPrice(request.price());
        return toBrandDto(brandRepository.save(brand));
    }

    @Transactional
    public void deleteBrand(Integer id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));
        imageService.deleteBrandFiles(id);
        if (brand.getProduct() != null) {
            brand.getProduct().getBrands().remove(brand);
        }
        brandRepository.delete(brand);
    }

    private SubCategory findSubCategory(Integer id) {
        return subCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subcategory not found"));
    }

    private ProductDto toProductDto(Product product) {
        return ProductMapper.convertProductToProductDto(product, imageService::createViewUrl);
    }

    private BrandDto toBrandDto(Brand brand) {
        return ProductMapper.convertBrandToBrandDto(brand, imageService::createViewUrl);
    }
}
