package com.product.service;

import com.product.dto.ProductDto;
import com.product.dto.ProductPriceResponse;
import org.springframework.stereotype.Service;

import java.util.List;
// Marks the product service layer; ProductServiceImpl provides the Spring-managed implementation.
@Service
public interface ProductService  {

     List<ProductDto> searchProducts(String keyword);

     ProductDto findByProductId(Integer id);

     ProductPriceResponse findBrandPrice(Integer productId, Integer brandId);

}
