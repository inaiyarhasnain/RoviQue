package com.order.controller;

import com.order.dto.OrderResponse;
import com.order.dto.PaymentStatusUpdateRequest;
import com.order.service.OrderService;
import com.order.security.JwtPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Tell Spring this class receives HTTP requests and returns API responses.
@RestController
// Add this URL prefix to all order endpoints.
@RequestMapping("/api/v1/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Map POST requests to creating an order from the caller's cart.
    @PostMapping("/all")
    public ResponseEntity<OrderResponse> createOrder(
            // Read the cart ID from a request header when one is provided.
            @RequestHeader(value = "X-CART-ID", required = false) String cartUuid,
            // Otherwise, read the cart ID from the browser cookie.
            @CookieValue(value = "CART_ID", required = false) String cartCookie,
            @AuthenticationPrincipal JwtPrincipal principal) {
        // Accept the cart ID from either the request header or browser cookie.
        if (cartUuid == null || cartUuid.isBlank()) {
            cartUuid = cartCookie;
        }
        // Create the order and return it with HTTP 201 (Created).
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(cartUuid, principal.userId()));
    }

    // Map GET requests to retrieving one order by its ID.
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            // Read the order ID from the URL path.
            @PathVariable Long id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        // Look up an existing order by its ID.
        boolean administrator = principal.roles().contains("ADMIN");
        return ResponseEntity.ok(orderService.getOrder(id, principal.userId(), administrator));
    }

    // Map PUT requests to updating only the payment status of an order.
    @PutMapping("/{id}/payment-status")
    public ResponseEntity<OrderResponse> updatePaymentStatus(
            @PathVariable Long id,
            @RequestBody PaymentStatusUpdateRequest request) {
        return ResponseEntity.ok(orderService.updatePaymentStatus(id, request));
    }

}
