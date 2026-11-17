package com.order.service;

import feign.FeignException;
import com.order.client.CartFeignClient;
import com.order.client.ProductFeignClient;
import com.order.dto.CartDto;
import com.order.dto.CartItemDto;
import com.order.dto.OrderItemResponse;
import com.order.dto.OrderResponse;
import com.order.dto.PaymentStatusUpdateRequest;
import com.order.dto.ProductPriceResponse;
import com.order.entity.Order;
import com.order.entity.OrderItem;
import com.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

// Register this class as a Spring service so the controller can use it.
@Service
public class OrderService {

    private final CartFeignClient cartFeignClient;
    private final ProductFeignClient productFeignClient;
    private final OrderRepository orderRepository;

    public OrderService(
            CartFeignClient cartFeignClient,
            ProductFeignClient productFeignClient,
            OrderRepository orderRepository) {
        this.cartFeignClient = cartFeignClient;
        this.productFeignClient = productFeignClient;
        this.orderRepository = orderRepository;
    }

    public OrderResponse createOrder(String cartUuid, Long userId) {
        // A cart ID is needed to know which cart should become an order.
        if (cartUuid == null || cartUuid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-CART-ID header is required");
        }
        requireUserId(userId);

        // Ask the cart service for the cart contents.
        CartDto cart = cartFeignClient.getCart(cartUuid);
        if (cart.getUserId() == null || !userId.equals(cart.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
        }
        // An empty cart cannot be checked out.
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create an order from an empty cart");
        }

        // Start building the order using the cart's owner and ID.
        Order order = new Order();
        order.setCartUuid(cart.getUuid());
        order.setUserId(userId);
        order.setStatus("PLACED");
        order.setCreatedAt(LocalDateTime.now());
        order.setTotalAmount(BigDecimal.ZERO);

        // Copy each cart item into the order and add its price to the total.
        for (CartItemDto cartItem : cart.getItems()) {
            if (cartItem.getProductId() == null
                    || cartItem.getProductId() <= 0
                    || cartItem.getProductId() > Integer.MAX_VALUE
                    || cartItem.getBrandId() == null
                    || cartItem.getBrandId() <= 0
                    || cartItem.getBrandId() > Integer.MAX_VALUE
                    || cartItem.getQuantity() == null
                    || cartItem.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Cart contains an invalid item");
            }

            ProductPriceResponse currentPrice;
            try {
                currentPrice = productFeignClient.getBrandPrice(
                        cartItem.getProductId(), cartItem.getBrandId());
            } catch (FeignException.NotFound exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "Cart contains a product or brand that no longer exists", exception);
            }
            if (currentPrice == null
                    || !cartItem.getProductId().equals(currentPrice.productId())
                    || !cartItem.getBrandId().equals(currentPrice.brandId())
                    || currentPrice.price() == null
                    || currentPrice.price().signum() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "Product service returned invalid pricing");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setBrandId(cartItem.getBrandId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(currentPrice.price());
            orderItem.setOrder(order);
            order.getItems().add(orderItem);

            order.setTotalAmount(order.getTotalAmount().add(
                    currentPrice.price().multiply(BigDecimal.valueOf(cartItem.getQuantity()))));
        }

        // Save the order before clearing the cart, then return the saved order.
        Order savedOrder = orderRepository.save(order);
        cartFeignClient.clearCart(cartUuid);
        return toResponse(savedOrder);
    }

    // This method only reads an order, so it does not save database changes.
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id, Long userId, boolean administrator) {
        // Find the order or return a 404 response if it does not exist.
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        requireUserId(userId);
        if (!administrator && !userId.equals(order.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForPayment(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return toResponse(order);
    }

    // Save the payment result reported by the payment service.
    @Transactional
    public OrderResponse updatePaymentStatus(Long id, PaymentStatusUpdateRequest request) {
        if (request == null || request.getPaymentStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment status is required");
        }

        String paymentStatus = request.getPaymentStatus().trim().toUpperCase(Locale.ROOT);
        List<String> allowedStatuses = List.of(
                "PENDING",
                "PAID",
                "REQUIRES_PAYMENT_METHOD",
                "REQUIRES_CONFIRMATION",
                "REQUIRES_ACTION",
                "PROCESSING",
                "REQUIRES_CAPTURE",
                "CANCELED");
        if (!allowedStatuses.contains(paymentStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment status is not valid");
        }

        Order order = orderRepository.findByIdForPaymentStatusUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if ("PAID".equals(order.getPaymentStatus())
                || ("CANCELED".equals(order.getPaymentStatus()) && !"PAID".equals(paymentStatus))) {
            return toResponse(order);
        }
        order.setPaymentStatus(paymentStatus);
        return toResponse(orderRepository.save(order));
    }

    private OrderResponse toResponse(Order order) {
        // Copy order details into a response object that can be sent as JSON.
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCartUuid(order.getCartUuid());
        response.setUserId(order.getUserId());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setCreatedAt(order.getCreatedAt());
        // Include each item's calculated line total in the response.
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> {
                    BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    return new OrderItemResponse(
                            item.getProductId(),
                            item.getBrandId(),
                            item.getQuantity(),
                            item.getPrice(),
                            lineTotal);
                })
                .toList();
        response.setItems(items);
        return response;
    }

    private void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT user ID is missing or invalid");
        }
    }
}
