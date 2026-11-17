package com.order.client;

import com.order.dto.CartDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Create an HTTP client that finds and calls the service named "cart" in Eureka.
@FeignClient(name = "cart")
public interface CartFeignClient {

    // Ask the cart service to return the cart with this UUID.
    @GetMapping("/api/v1/cart/{uuid}")
    // Put the method argument into the {uuid} part of the URL.
    CartDto getCart(@PathVariable("uuid") String uuid);

    // Ask the cart service to remove the items from this cart.
    @DeleteMapping("/api/v1/cart/{uuid}/items")
    // Put the method argument into the {uuid} part of the URL.
    void clearCart(@PathVariable("uuid") String uuid);
}
