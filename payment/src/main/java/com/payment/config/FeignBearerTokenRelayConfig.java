package com.payment.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignBearerTokenRelayConfig {

    @Bean
    RequestInterceptor bearerTokenRelayInterceptor(
            @Value("${security.internal.payment-token:}") String paymentServiceToken) {
        return template -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                String authorization = servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (authorization != null && authorization.startsWith("Bearer ")) {
                    template.removeHeader(HttpHeaders.AUTHORIZATION);
                    template.header(HttpHeaders.AUTHORIZATION, authorization);
                }
            }
            boolean paymentStatusUpdate = "PUT".equalsIgnoreCase(template.method())
                    && template.path().endsWith("/payment-status");
            boolean internalOrderRead = "GET".equalsIgnoreCase(template.method())
                    && template.path().startsWith("/api/v1/internal/orders/");
            if (paymentStatusUpdate || internalOrderRead) {
                if (paymentServiceToken == null || paymentServiceToken.isBlank()) {
                    throw new IllegalStateException(
                            "INTERNAL_PAYMENT_TOKEN must be configured for payment status updates");
                }
                template.header("X-Internal-Service-Token", paymentServiceToken);
            }
        };
    }
}
