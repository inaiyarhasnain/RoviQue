package com.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

// Enable Spring Boot setup and automatic component scanning.
@SpringBootApplication
// Register this order service with Eureka.
@EnableDiscoveryClient
// Find Feign clients used to call other services.
@EnableFeignClients
public class OrderApplication {

    public static void main(String[] args) {
        // Start the order service and register it with service discovery.
        SpringApplication.run(OrderApplication.class, args);
    }

}
