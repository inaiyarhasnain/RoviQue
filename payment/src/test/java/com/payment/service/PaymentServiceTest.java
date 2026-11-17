package com.payment.service;

import com.payment.client.OrderFeignClient;
import com.payment.dto.OrderItemPaymentDetails;
import com.payment.dto.OrderPaymentDetails;
import com.payment.dto.PaymentStatusUpdateRequest;
import com.stripe.StripeClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private static final String WEBHOOK_SECRET = "whsec_test";

    @Test
    void checksOrderOwnershipBeforeRequestingPaymentStatus() {
        StripeClient stripeClient = mock(StripeClient.class);
        OrderFeignClient orderClient = mock(OrderFeignClient.class);
        when(orderClient.getOrder(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        PaymentService paymentService = new PaymentService(stripeClient, orderClient, WEBHOOK_SECRET);

        assertThrows(ResponseStatusException.class, () -> paymentService.getCheckoutStatus(99L));

        verify(orderClient).getOrder(99L);
        verifyNoInteractions(stripeClient);
    }

    @Test
    void rejectsWebhookWithInvalidSignatureBeforeReadingOrders() {
        OrderFeignClient orderClient = mock(OrderFeignClient.class);
        PaymentService paymentService = new PaymentService(mock(StripeClient.class), orderClient, WEBHOOK_SECRET);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> paymentService.handleStripeWebhook("{\"id\":\"evt_test\"}", "t=1,v1=invalid"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(orderClient);
    }

    @Test
    void verifiesPaymentIntentAndOrderBeforeMarkingTheOrderPaid() throws Exception {
        OrderFeignClient orderClient = mock(OrderFeignClient.class);
        StripeClient stripeClient = mock(StripeClient.class);
        OrderPaymentDetails order = new OrderPaymentDetails(
                17L,
                "cart-uuid",
                new java.math.BigDecimal("12.34"),
                "PENDING",
                List.of(new OrderItemPaymentDetails(
                        3L, 4L, 2, new java.math.BigDecimal("6.17"), new java.math.BigDecimal("12.34"))));
        when(orderClient.getOrderForWebhook(17L)).thenReturn(order);
        PaymentService paymentService = new PaymentService(stripeClient, orderClient, WEBHOOK_SECRET);
        String payload = """
                {
                  "id": "evt_test",
                  "object": "event",
                  "api_version": "2024-06-20",
                  "created": %d,
                  "data": {
                    "object": {
                      "id": "pi_test",
                      "object": "payment_intent",
                      "amount": 1234,
                      "currency": "usd",
                      "metadata": {"orderId": "17"},
                      "status": "succeeded"
                    }
                  },
                  "livemode": false,
                  "pending_webhooks": 1,
                  "type": "payment_intent.succeeded"
                }
                """.formatted(Instant.now().getEpochSecond());

        var result = paymentService.handleStripeWebhook(payload, signature(payload, WEBHOOK_SECRET));

        assertEquals("processed", result.status());
        verify(orderClient).getOrderForWebhook(17L);
        verify(orderClient).updatePaymentStatus(17L, new PaymentStatusUpdateRequest("PAID"));
    }

    private String signature(String payload, String secret) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(digest);
    }
}
