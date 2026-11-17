package com.cart.controller;

 import com.cart.dto.AddToCartRequest;
 import com.cart.dto.CartResponse;
 import com.cart.entity.Cart;
 import com.cart.security.JwtPrincipal;
 import com.cart.service.CartService;
 import jakarta.validation.Valid;
 import org.springframework.http.HttpHeaders;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.ResponseCookie;
 import org.springframework.http.ResponseEntity;
 import org.springframework.security.core.annotation.AuthenticationPrincipal;
 import org.springframework.web.bind.annotation.*;
 import org.springframework.web.server.ResponseStatusException;

 import java.time.Duration;

 // Tell Spring this class receives HTTP requests and returns API responses.
 @RestController
 // Add this URL prefix to all cart endpoints.
 @RequestMapping("/api/v1/cart")
 // Allow browser apps from different origins to call these endpoints.
 @CrossOrigin(origins = "*", exposedHeaders = "X-CART-ID")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // Map POST requests to this path to adding an item to a cart.
    @PostMapping("/add")
    public ResponseEntity<String> addToCart(
            @AuthenticationPrincipal JwtPrincipal principal,
            // Read the cart ID from a request header when the caller provides it.
            @RequestHeader(value = "X-CART-ID", required = false) String uuid,
            // Otherwise, Spring reads the cart ID from this browser cookie.
            @CookieValue(value = "CART_ID", required = false) String cartIdCookie,
            // Convert the JSON request body into an AddToCartRequest object.
            @Valid @RequestBody AddToCartRequest request
    ) {

        // Use the request header first, then fall back to the browser's cart cookie.
        if (uuid == null || uuid.isBlank()) {
            uuid = cartIdCookie;
        }

        // Add the item and get the cart ID created or found by the service.
        Cart cart = cartService.addToCart(uuid, request, principal.userId());

        // Return the cart ID in both a header and a cookie for the next request.
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-CART-ID", cart.getUuid());
        headers.add("Access-Control-Expose-Headers", "X-CART-ID");
        headers.add(HttpHeaders.SET_COOKIE, ResponseCookie.from("CART_ID", cart.getUuid())
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(30))
                .sameSite("Lax")
                .build()
                .toString());

        return ResponseEntity.ok()
                .headers(headers)
                .body("Product added to cart");
    }

    // Map GET requests to retrieving a cart by its ID.
    @GetMapping("/{uuid}")
    // Read the cart ID from the URL path.
    public ResponseEntity<CartResponse> getCart(
            @PathVariable String uuid,
            @AuthenticationPrincipal JwtPrincipal principal) {
        // Return the cart that belongs to this cart ID.
        return ResponseEntity.ok(cartService.getCart(uuid, principal.userId()));
    }

    // Map DELETE requests to removing all items from a cart.
    @DeleteMapping("/{uuid}/items")
    // Read the cart ID from the URL path.
    public ResponseEntity<Void> clearCart(
            @PathVariable String uuid,
            @AuthenticationPrincipal JwtPrincipal principal) {
        // Remove all items but keep the cart itself.
        cartService.clearCart(uuid, principal.userId());
        return ResponseEntity.noContent().build();
    }

}
