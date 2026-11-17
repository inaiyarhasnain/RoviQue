package com.payment.service;

import com.google.gson.JsonSyntaxException;
import com.payment.client.OrderFeignClient;
import com.payment.dto.CheckoutSessionResponse;
import com.payment.dto.CreateCheckoutSessionRequest;
import com.payment.dto.OrderItemPaymentDetails;
import com.payment.dto.OrderPaymentDetails;
import com.payment.dto.CheckoutStatusResponse;
import com.payment.dto.PaymentStatusUpdateRequest;
import com.payment.dto.StripeWebhookResponse;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.StripeSearchResult;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentSearchParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Register this class as a Spring service so the controller can use it.
@Service
public class PaymentService {

    private static final String CURRENCY = "usd";

    private final StripeClient stripeClient;
    private final OrderFeignClient orderClient;
    private final String webhookSigningSecret;

    public PaymentService(
            StripeClient stripeClient,
            OrderFeignClient orderClient,
            @Value("${stripe.webhook.secret}") String webhookSigningSecret) {
        this.stripeClient = stripeClient;
        this.orderClient = orderClient;
        this.webhookSigningSecret = webhookSigningSecret;
        if (webhookSigningSecret == null || webhookSigningSecret.isBlank()) {
            throw new IllegalStateException("STRIPE_WEBHOOK_SECRET must be configured.");
        }
    }

    public CheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request) {
        // Check the request and its return URLs before contacting other services.
        Long orderId = requireOrderId(request == null ? null : request.orderId());
        validateRedirectUrl(request.successUrl(), "Success URL");
        validateRedirectUrl(request.cancelUrl(), "Cancel URL");

        // Get the saved order and its product lines from the order service.
        OrderPaymentDetails order = getOrder(orderId);
        BigDecimal amount = order.totalAmount();

        BigDecimal itemTotals = order.items().stream()
                .map(item -> {
                    validateOrderItem(item);
                    return item.lineTotal();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (itemTotals.compareTo(amount) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Order total does not match its product items");
        }

        // Build Stripe's hosted checkout page and tag it with the order ID for status checks.
        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(orderId.toString())
                // Add the order ID so the success page can check its payment status.
                .setSuccessUrl(addOrderId(request.successUrl(), orderId))
                .setCancelUrl(request.cancelUrl())
                .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                        .putMetadata("orderId", orderId.toString())
                        .build());

        // Add each saved order product separately so Stripe shows what is being purchased.
        for (OrderItemPaymentDetails item : order.items()) {
            paramsBuilder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(item.quantity().longValue())
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(CURRENCY)
                            .setUnitAmount(toCents(item.price()))
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Product " + item.productId())
                                    .build())
                            .build())
                    .build());
        }

        SessionCreateParams params = paramsBuilder.build();

        try {
            // Ask Stripe to create the checkout page and return its URL.
            Session session = stripeClient.v1().checkout().sessions().create(params);
            // Mark this order as awaiting payment in the order service.
            orderClient.updatePaymentStatus(orderId, new PaymentStatusUpdateRequest("PENDING"));
            return new CheckoutSessionResponse(
                    "Checkout link created. Open paymentUrl to complete payment.",
                    session.getId(),
                    session.getUrl(),
                    amount,
                    CURRENCY);
        } catch (StripeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Unable to create Stripe Checkout session", exception);
        }
    }

    public CheckoutStatusResponse getCheckoutStatus(Long orderId) {
        // Reject an invalid order ID before making a request to Stripe.
        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid order ID is required");
        }
        OrderPaymentDetails order = getOrder(orderId);

        try {
            // Find Stripe payments marked with this order ID.
            PaymentIntentSearchParams params = PaymentIntentSearchParams.builder()
                    .setQuery("metadata['orderId']:'" + orderId + "'")
                    .setLimit(100L)
                    .build();
            StripeSearchResult<PaymentIntent> result =
                    stripeClient.v1().paymentIntents().search(params);
            List<PaymentIntent> intents = result.getData();
            // Stripe marks a completed card payment as "succeeded".
            boolean paid = intents.stream().anyMatch(intent -> "succeeded".equals(intent.getStatus()))
                    || "PAID".equals(order.paymentStatus());
            String status = intents.stream()
                    .map(PaymentIntent::getStatus)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse("unpaid");
            String orderPaymentStatus = paid
                    ? "PAID"
                    : ("unpaid".equals(status) ? "PENDING" : status.toUpperCase(java.util.Locale.ROOT));
            if (shouldAdvanceStatus(order.paymentStatus(), orderPaymentStatus)) {
                orderClient.updatePaymentStatus(
                        orderId,
                        new PaymentStatusUpdateRequest(orderPaymentStatus));
            }
            String message = paid
                    ? "Payment completed successfully for order " + orderId + "."
                    : "Payment has not been completed for order " + orderId + ".";
            return new CheckoutStatusResponse(
                    orderId, paid, message, paid ? "paid" : status.toLowerCase(java.util.Locale.ROOT));
        } catch (StripeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Unable to check payment status with Stripe", exception);
        }
    }

    public StripeWebhookResponse handleStripeWebhook(String payload, String signature) {
        if (payload == null || payload.isBlank() || signature == null || signature.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe webhook payload and signature are required");
        }

        Event event;
        try {
            // Stripe signs the raw request body; always verify it before trusting event data.
            event = Webhook.constructEvent(payload, signature, webhookSigningSecret);
        } catch (SignatureVerificationException | JsonSyntaxException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Stripe webhook signature or payload is invalid", exception);
        }

        String paymentStatus = statusForEvent(event.getType());
        if (paymentStatus == null) {
            return new StripeWebhookResponse("ignored", "Stripe event type is not handled.");
        }

        var deserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject;
        if (deserializer.getObject().isPresent()) {
            stripeObject = deserializer.getObject().get();
        } else {
            try {
                // Stripe permits unsafe deserialization when API versions differ; below we still
                // validate the payment intent identity, amount, currency, and status explicitly.
                stripeObject = deserializer.deserializeUnsafe();
            } catch (EventDataObjectDeserializationException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Stripe webhook does not contain a valid payment intent", exception);
            }
        }
        if (!(stripeObject instanceof PaymentIntent paymentIntent)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Stripe webhook does not contain a payment intent");
        }

        Long orderId = orderIdFromMetadata(paymentIntent.getMetadata());
        OrderPaymentDetails order = getOrderForWebhook(orderId);
        long expectedAmount = toCents(order.totalAmount());
        if (paymentIntent.getAmount() == null
                || paymentIntent.getAmount() != expectedAmount
                || !CURRENCY.equalsIgnoreCase(paymentIntent.getCurrency())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Stripe payment amount or currency does not match the order");
        }
        validateEventPaymentStatus(event.getType(), paymentIntent.getStatus());

        if (shouldAdvanceStatus(order.paymentStatus(), paymentStatus)) {
            orderClient.updatePaymentStatus(orderId, new PaymentStatusUpdateRequest(paymentStatus));
        }
        return new StripeWebhookResponse("processed", "Stripe payment event was verified.");
    }

    private Long requireOrderId(Long orderId) {
        if (orderId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order ID is required");
        }
        return orderId;
    }

    private OrderPaymentDetails getOrder(Long orderId) {
        // Read the saved order so its cart, products, and total are not supplied by the browser.
        OrderPaymentDetails order = orderClient.getOrder(orderId);
        if (order == null
                || order.id() == null
                || !orderId.equals(order.id())
                || order.cartUuid() == null
                || order.cartUuid().isBlank()
                || order.totalAmount() == null
                || order.totalAmount().signum() <= 0
                || order.items() == null
                || order.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order has invalid payment details");
        }
        return order;
    }

    private OrderPaymentDetails getOrderForWebhook(Long orderId) {
        OrderPaymentDetails order = orderClient.getOrderForWebhook(orderId);
        if (order == null
                || order.id() == null
                || !orderId.equals(order.id())
                || order.cartUuid() == null
                || order.cartUuid().isBlank()
                || order.totalAmount() == null
                || order.totalAmount().signum() <= 0
                || order.items() == null
                || order.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order has invalid payment details");
        }

        BigDecimal itemTotals = order.items().stream()
                .map(item -> {
                    validateOrderItem(item);
                    return item.lineTotal();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (itemTotals.compareTo(order.totalAmount()) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Order total does not match its product items");
        }
        return order;
    }

    private Long orderIdFromMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.get("orderId") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe payment is missing order metadata");
        }
        long orderId;
        try {
            orderId = Long.parseLong(metadata.get("orderId"));
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Stripe payment has an invalid order reference", exception);
        }
        if (orderId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe payment has an invalid order reference");
        }
        return orderId;
    }

    private String statusForEvent(String eventType) {
        return switch (eventType) {
            case "payment_intent.succeeded" -> "PAID";
            case "payment_intent.payment_failed" -> "REQUIRES_PAYMENT_METHOD";
            case "payment_intent.canceled" -> "CANCELED";
            default -> null;
        };
    }

    private void validateEventPaymentStatus(String eventType, String stripeStatus) {
        boolean matches = switch (eventType) {
            case "payment_intent.succeeded" -> "succeeded".equals(stripeStatus);
            case "payment_intent.payment_failed" -> "requires_payment_method".equals(stripeStatus);
            case "payment_intent.canceled" -> "canceled".equals(stripeStatus);
            default -> false;
        };
        if (!matches) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Stripe event type does not match the payment status");
        }
    }

    private boolean shouldAdvanceStatus(String currentStatus, String newStatus) {
        if ("PAID".equals(currentStatus)) {
            return false;
        }
        return !"CANCELED".equals(currentStatus) || "PAID".equals(newStatus);
    }

    private void validateOrderItem(OrderItemPaymentDetails item) {
        if (item == null
                || item.productId() == null
                || item.quantity() == null
                || item.quantity() <= 0
                || item.price() == null
                || item.price().signum() <= 0
                || item.lineTotal() == null
                || item.lineTotal().compareTo(item.price().multiply(BigDecimal.valueOf(item.quantity()))) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order contains an invalid product item");
        }
    }

    private long toCents(BigDecimal amount) {
        try {
            // Stripe expects USD in cents, so convert the dollar amount to a whole number.
            return amount.setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order total is invalid", exception);
        }
    }

    private void validateRedirectUrl(String url, String name) {
        try {
            // Stripe needs a complete HTTP or HTTPS address for the redirect.
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException();
            }
            URI uri = URI.create(url);
            if (uri.getHost() == null
                    || uri.getUserInfo() != null
                    || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, name + " must be an absolute HTTP or HTTPS URL", exception);
        }
    }

    private String addOrderId(String url, Long orderId) {
        // Keep any URL fragment at the end while adding the order ID as a query parameter.
        int fragmentStart = url.indexOf('#');
        String fragment = fragmentStart >= 0 ? url.substring(fragmentStart) : "";
        String baseUrl = fragmentStart >= 0 ? url.substring(0, fragmentStart) : url;
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "orderId=" + orderId + fragment;
    }
}
