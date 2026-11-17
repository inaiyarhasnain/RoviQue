package com.product.service;

import com.product.dto.ProductDto;
import com.product.dto.ProductPriceResponse;
import com.product.entity.Brand;
import com.product.entity.Product;
import com.product.mapper.ProductMapper;
import com.product.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
// Register this class as a Spring-managed service.
@Service
public class ProductServiceImpl implements ProductService{

    private final ProductRepository productRepository;
    private final ImageService imageService;

    public ProductServiceImpl(ProductRepository productRepository, ImageService imageService) {
        this.productRepository = productRepository;
        this.imageService = imageService;
    }

    // Implement the search operation declared by ProductService.
    @Override
    // This method reads data only, so it does not save database changes.
    @Transactional(readOnly = true)
    public List<ProductDto> searchProducts(String keyword) {
        // Ask the repository to find matching products in the database.
        List<Product> products = productRepository.searchProducts(keyword);

        // Convert each database entity into a response object for the API.
        List<ProductDto> productDtoList = new ArrayList<>();
        for (Product p : products){
            productDtoList.add(ProductMapper.convertProductToProductDto(p, imageService::createViewUrl));
        }
        return productDtoList;

    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto findByProductId(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        return ProductMapper.convertProductToProductDto(product, imageService::createViewUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductPriceResponse findBrandPrice(Integer productId, Integer brandId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        Brand brand = product.getBrands().stream()
                .filter(candidate -> brandId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product brand not found"));
        if (brand.getPrice() == null || brand.getPrice().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product brand has no valid price");
        }
        return new ProductPriceResponse(productId.longValue(), brandId.longValue(), brand.getPrice());
    }
}
