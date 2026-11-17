package com.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Holds the payment status sent by the payment service.
@Getter
@Setter
@NoArgsConstructor
public class PaymentStatusUpdateRequest {
    private String paymentStatus;
}
