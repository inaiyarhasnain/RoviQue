package com.cart.controller;

import com.cart.dto.AddToCartRequest;
import com.cart.entity.Cart;
import com.cart.security.JwtPrincipal;
import com.cart.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartControllerTest {

    // Tell JUnit to run this cart-controller behavior check as a test.
    @Test
    void usesCartCookieWhenHeaderIsMissingAndReturnsUpdatedCookie() {
        // Prepare a cart request and a service mock with a known cart ID.
        String uuid = "3bf7921e-820e-41b3-95a0-3709e9a1";
        AddToCartRequest request = new AddToCartRequest();
        Cart cart = new Cart();
        cart.setUuid(uuid);
        JwtPrincipal principal = new JwtPrincipal(42L, "user@example.com", java.util.List.of("USER"));

        CartService cartService = mock(CartService.class);
        when(cartService.addToCart(uuid, request, 42L)).thenReturn(cart);

        // Call the controller and check the cart ID is returned in its headers and cookie.
        CartController controller = new CartController(cartService);
        ResponseEntity<String> response = controller.addToCart(principal, null, uuid, request);

        verify(cartService).addToCart(uuid, request, 42L);
        assertEquals(uuid, response.getHeaders().getFirst("X-CART-ID"));
        assertTrue(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).contains("CART_ID=" + uuid));
    }
}
