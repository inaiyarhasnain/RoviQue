package com.cart.service;

import feign.FeignException;
import com.cart.client.ProductFeignClient;
import com.cart.dto.AddToCartRequest;
import com.cart.dto.CartItemResponse;
import com.cart.dto.CartResponse;
import com.cart.dto.ProductPriceResponse;
import com.cart.entity.Cart;
import com.cart.entity.CartItem;
import com.cart.repository.CartRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

// Register this class as a Spring service so the controller can use it.
@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductFeignClient productFeignClient;

    public CartService(CartRepository cartRepository, ProductFeignClient productFeignClient) {
        this.cartRepository = cartRepository;
        this.productFeignClient = productFeignClient;
    }

    @Transactional
    public Cart addToCart(String uuid, AddToCartRequest request, Long userId) {
        requireUserId(userId);
        if (request == null
                || request.getProductId() == null
                || request.getProductId() <= 0
                || request.getProductId() > Integer.MAX_VALUE
                || request.getBrandId() == null
                || request.getBrandId() <= 0
                || request.getBrandId() > Integer.MAX_VALUE
                || request.getQuantity() == null
                || request.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart item is invalid");
        }
        ProductPriceResponse productPrice;
        try {
            productPrice = productFeignClient.getBrandPrice(request.getProductId(), request.getBrandId());
        } catch (FeignException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product or brand was not found", exception);
        }
        if (productPrice == null
                || !request.getProductId().equals(productPrice.productId())
                || !request.getBrandId().equals(productPrice.brandId())
                || productPrice.price() == null
                || productPrice.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Product service returned invalid pricing");
        }

        Cart cart;

        if (uuid == null || uuid.isBlank()) {
            uuid = UUID.randomUUID().toString();
            cart = new Cart();
            cart.setUuid(uuid);
        } else {
            Optional<Cart> optionalCart = cartRepository.findByUuidForUpdate(uuid);
            if (optionalCart.isPresent()) {
                cart = optionalCart.get();
                requireOwnerOrClaimLegacyCart(cart, userId);
            } else {
                cart = new Cart();
                cart.setUuid(uuid);
            }
        }
        cart.setUserId(userId);

        // If this product is already in the cart, increase its quantity.
        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> request.getProductId().equals(item.getProductId())
                        && request.getBrandId().equals(item.getBrandId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Existing cart item is invalid");
            }
            try {
                item.setQuantity(Math.addExact(item.getQuantity(), request.getQuantity()));
            } catch (ArithmeticException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart quantity is too large", exception);
            }
            item.setPrice(productPrice.price());
        } else {
            CartItem newItem = new CartItem();
            newItem.setProductId(request.getProductId());
            newItem.setBrandId(request.getBrandId());
            newItem.setQuantity(request.getQuantity());
            newItem.setPrice(productPrice.price());
            newItem.setCart(cart);

            cart.getItems().add(newItem);
        }

        // Save the cart and its items, then return the saved cart.
        return cartRepository.save(cart);
    }

    // This method only reads the cart, so it does not save database changes.
    @Transactional(readOnly = true)
    public CartResponse getCart(String uuid, Long userId) {
        requireUserId(userId);
        // Find the cart or tell the caller it does not exist.
        Cart cart = cartRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
        requireOwner(cart, userId);

        // Copy database data into the response sent by the API.
        CartResponse response = new CartResponse();
        response.setUuid(cart.getUuid());
        response.setUserId(cart.getUserId());
        response.setItems(cart.getItems().stream()
                .map(item -> new CartItemResponse(
                        item.getProductId(),
                        item.getBrandId(),
                        item.getQuantity(),
                        item.getPrice()))
                .toList());
        return response;
    }

    // Run cart changes in a database transaction.
    @Transactional
    public void clearCart(String uuid, Long userId) {
        requireUserId(userId);
        // Find the cart, remove its items, and save the empty cart.
        Cart cart = cartRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
        requireOwner(cart, userId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT user ID is missing or invalid");
        }
    }

    private void requireOwnerOrClaimLegacyCart(Cart cart, Long userId) {
        if (cart.getUserId() == null) {
            cart.setUserId(userId);
            return;
        }
        requireOwner(cart, userId);
    }

    private void requireOwner(Cart cart, Long userId) {
        if (!userId.equals(cart.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
        }
    }
}
