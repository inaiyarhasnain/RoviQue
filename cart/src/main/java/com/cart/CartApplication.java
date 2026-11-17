package com.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

// Enable Spring Boot configuration and automatic component scanning.
@SpringBootApplication
// Register this cart service with Eureka for service discovery.
@EnableDiscoveryClient
// Find Feign clients used to call other services.
@EnableFeignClients
public class CartApplication {

    public static void main(String[] args) {
        // Start the cart service and register it with service discovery.
        SpringApplication.run(CartApplication.class, args);
    }

}
