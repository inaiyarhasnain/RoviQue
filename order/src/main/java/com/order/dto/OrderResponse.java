package com.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Lombok creates methods to read and update the response fields.
@Getter
@Setter
// Lombok creates an empty constructor for JSON conversion.
@NoArgsConstructor
public class OrderResponse {
    private Long id;
    private String cartUuid;
    private Long userId;
    private BigDecimal totalAmount;
    private String status;
    private String paymentStatus;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
}
