package com.payment.client;

import com.payment.dto.OrderPaymentDetails;
import com.payment.dto.PaymentStatusUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Create an HTTP client that finds and calls the service named "order" in Eureka.
@FeignClient(name = "order")
public interface OrderFeignClient {

    // Ask the order service to return the order and its total amount.
    @GetMapping("/api/v1/order/{id}")
    // Put the method argument into the {id} part of the URL.
    OrderPaymentDetails getOrder(@PathVariable("id") Long id);

    @GetMapping("/api/v1/internal/orders/{id}")
    OrderPaymentDetails getOrderForWebhook(@PathVariable("id") Long id);

    // Send the Stripe-verified payment status to the order service.
    @PutMapping("/api/v1/order/{id}/payment-status")
    void updatePaymentStatus(
            @PathVariable("id") Long id,
            @RequestBody PaymentStatusUpdateRequest request);
}
