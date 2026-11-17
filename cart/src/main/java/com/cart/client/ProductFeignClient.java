package com.cart.client;

import com.cart.dto.ProductPriceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product")
public interface ProductFeignClient {

    @GetMapping("/api/v1/product/{productId}/brands/{brandId}/price")
    ProductPriceResponse getBrandPrice(
            @PathVariable("productId") Long productId,
            @PathVariable("brandId") Long brandId);
}
