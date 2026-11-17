package com.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

// Find Feign clients so this service can call other services.
@EnableFeignClients
// Let this service register with and find services through Eureka.
@EnableDiscoveryClient
// Enable Spring Boot setup and automatic component scanning.
@SpringBootApplication
public class ProductApplication {

    public static void main(String[] args) {
        // Start the product service and register it with service discovery.
        SpringApplication.run(ProductApplication.class, args);
    }

}
