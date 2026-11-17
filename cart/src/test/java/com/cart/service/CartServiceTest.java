package com.cart.service;

import com.cart.client.ProductFeignClient;
import com.cart.dto.AddToCartRequest;
import com.cart.dto.ProductPriceResponse;
import com.cart.entity.Cart;
import com.cart.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceTest {

    @Test
    void assignsAuthenticatedOwnerAndUsesProductServicePrice() {
        CartRepository cartRepository = mock(CartRepository.class);
        ProductFeignClient productClient = mock(ProductFeignClient.class);
        AddToCartRequest request = new AddToCartRequest(12L, 3L, 2, new BigDecimal("0.01"));
        when(productClient.getBrandPrice(12L, 3L))
                .thenReturn(new ProductPriceResponse(12L, 3L, new BigDecimal("14.50")));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart cart = new CartService(cartRepository, productClient).addToCart(null, request, 42L);

        assertEquals(42L, cart.getUserId());
        assertEquals(new BigDecimal("14.50"), cart.getItems().get(0).getPrice());
        assertEquals(2, cart.getItems().get(0).getQuantity());
    }

    @Test
    void doesNotAllowAnotherUserToReadCart() {
        CartRepository cartRepository = mock(CartRepository.class);
        ProductFeignClient productClient = mock(ProductFeignClient.class);
        Cart cart = new Cart();
        cart.setUuid("cart-id");
        cart.setUserId(7L);
        when(cartRepository.findByUuid("cart-id")).thenReturn(Optional.of(cart));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> new CartService(cartRepository, productClient).getCart("cart-id", 42L));

        assertEquals(404, exception.getStatusCode().value());
        verify(cartRepository).findByUuid("cart-id");
    }

    @Test
    void doesNotAllowAnotherUserToAddToCart() {
        CartRepository cartRepository = mock(CartRepository.class);
        ProductFeignClient productClient = mock(ProductFeignClient.class);
        AddToCartRequest request = new AddToCartRequest(12L, 3L, 1, null);
        Cart cart = new Cart();
        cart.setUuid("cart-id");
        cart.setUserId(7L);
        when(productClient.getBrandPrice(12L, 3L))
                .thenReturn(new ProductPriceResponse(12L, 3L, new BigDecimal("14.50")));
        when(cartRepository.findByUuidForUpdate("cart-id")).thenReturn(Optional.of(cart));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> new CartService(cartRepository, productClient).addToCart("cart-id", request, 42L));

        assertEquals(404, exception.getStatusCode().value());
        verify(cartRepository, never()).save(any(Cart.class));
    }
}
