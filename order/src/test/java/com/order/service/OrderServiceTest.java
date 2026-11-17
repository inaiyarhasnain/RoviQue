package com.order.service;

import com.order.client.CartFeignClient;
import com.order.client.ProductFeignClient;
import com.order.dto.CartDto;
import com.order.dto.CartItemDto;
import com.order.dto.ProductPriceResponse;
import com.order.dto.PaymentStatusUpdateRequest;
import com.order.entity.Order;
import com.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    // Tell JUnit to run this order-creation behavior check as a test.
    @Test
    void createsOrderFromCartAndClearsCart() {
        // Prepare cart data that the mocked cart service will return.
        String cartUuid = "cart-uuid";
        CartItemDto cartItem = new CartItemDto();
        cartItem.setProductId(101L);
        cartItem.setBrandId(4L);
        cartItem.setQuantity(3);
        cartItem.setPrice(new BigDecimal("0.01"));

        CartDto cart = new CartDto();
        cart.setUuid(cartUuid);
        cart.setUserId(42L);
        cart.setItems(List.of(cartItem));

        // Use mocks so this test does not call another service or a real database.
        CartFeignClient cartClient = mock(CartFeignClient.class);
        ProductFeignClient productClient = mock(ProductFeignClient.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        when(cartClient.getCart(cartUuid)).thenReturn(cart);
        when(productClient.getBrandPrice(101L, 4L))
                .thenReturn(new ProductPriceResponse(101L, 4L, new BigDecimal("24999.00")));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Create the order, then check its totals and confirm that the cart is cleared.
        OrderService orderService = new OrderService(cartClient, productClient, orderRepository);
        var response = orderService.createOrder(cartUuid, 42L);

        assertEquals(new BigDecimal("74997.00"), response.getTotalAmount());
        assertEquals(1, response.getItems().size());
        assertEquals(new BigDecimal("74997.00"), response.getItems().get(0).getLineTotal());
        verify(cartClient).clearCart(cartUuid);
    }

    @Test
    void refusesToCreateAnOrderFromAnotherUsersCart() {
        String cartUuid = "cart-uuid";
        CartDto cart = new CartDto();
        cart.setUuid(cartUuid);
        cart.setUserId(7L);

        CartFeignClient cartClient = mock(CartFeignClient.class);
        ProductFeignClient productClient = mock(ProductFeignClient.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        when(cartClient.getCart(cartUuid)).thenReturn(cart);

        OrderService orderService = new OrderService(cartClient, productClient, orderRepository);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.createOrder(cartUuid, 42L));

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void refusesToReturnAnotherUsersOrder() {
        Order order = new Order();
        order.setId(3L);
        order.setUserId(7L);
        OrderRepository orderRepository = mock(OrderRepository.class);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        OrderService orderService = new OrderService(
                mock(CartFeignClient.class), mock(ProductFeignClient.class), orderRepository);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.getOrder(3L, 42L, false));

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void doesNotRegressPaidOrderStatus() {
        Order order = new Order();
        order.setId(3L);
        order.setPaymentStatus("PAID");
        OrderRepository orderRepository = mock(OrderRepository.class);
        when(orderRepository.findByIdForPaymentStatusUpdate(3L)).thenReturn(Optional.of(order));

        OrderService orderService = new OrderService(
                mock(CartFeignClient.class), mock(ProductFeignClient.class), orderRepository);
        PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest();
        request.setPaymentStatus("PENDING");

        var response = orderService.updatePaymentStatus(3L, request);

        assertEquals("PAID", response.getPaymentStatus());
        verify(orderRepository, never()).save(any(Order.class));
    }
}
