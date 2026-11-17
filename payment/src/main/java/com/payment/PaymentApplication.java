package com.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

// Enable Spring Boot setup and automatic component scanning.
@SpringBootApplication
// Register this payment service with Eureka.
@EnableDiscoveryClient
// Find Feign clients used to call other services.
@EnableFeignClients
public class PaymentApplication {

    public static void main(String[] args) {
        // Start the payment service and register it with service discovery.
        SpringApplication.run(PaymentApplication.class, args);
    }

}
