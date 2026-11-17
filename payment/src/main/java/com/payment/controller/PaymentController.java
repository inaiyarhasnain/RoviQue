package com.payment.controller;

import com.payment.dto.CheckoutSessionResponse;
import com.payment.dto.CreateCheckoutSessionRequest;
import com.payment.dto.CheckoutStatusResponse;
import com.payment.dto.StripeWebhookResponse;
import com.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Tell Spring this class receives HTTP requests and returns API responses.
@RestController
// Add this URL prefix to both payment endpoints.
@RequestMapping("/api/v1/payment")
public class PaymentController{

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // Map POST requests to creating a Stripe checkout link.
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            // Convert the JSON request body into the checkout request object.
            @RequestBody CreateCheckoutSessionRequest request) {
        // Create a hosted Stripe checkout link for the requested order.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createCheckoutSession(request));
    }

    // Map GET requests to checking payment status for an order.
    @GetMapping("/orders/{orderId}/status")
    public ResponseEntity<CheckoutStatusResponse> getPaymentStatus(
            // Read the order ID from the URL path.
            @PathVariable Long orderId) {
        // Check Stripe to see whether this order's payment has completed.
        return ResponseEntity.ok(paymentService.getCheckoutStatus(orderId));
    }

    @PostMapping("/webhook")
    public ResponseEntity<StripeWebhookResponse> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        return ResponseEntity.ok(paymentService.handleStripeWebhook(payload, signature));
    }
}
