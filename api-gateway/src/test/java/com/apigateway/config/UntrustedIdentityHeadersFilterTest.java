package com.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UntrustedIdentityHeadersFilterTest {

    @Test
    void removesClientSuppliedIdentityHeadersBeforeForwarding() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/cart/test")
                        .header("X-User-Id", "999")
                        .header("X-Forwarded-User", "admin@example.com")
                        .header("X-Internal-Service-Token", "untrusted")
                        .header("Authorization", "Bearer signed-token")
                        .build());
        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();

        new UntrustedIdentityHeadersFilter()
                .filter(exchange, next -> {
                    forwardedExchange.set(next);
                    return reactor.core.publisher.Mono.empty();
                })
                .block();

        assertNotNull(forwardedExchange.get());
        assertNull(forwardedExchange.get().getRequest().getHeaders().getFirst("X-User-Id"));
        assertNull(forwardedExchange.get().getRequest().getHeaders().getFirst("X-Forwarded-User"));
        assertNull(forwardedExchange.get().getRequest().getHeaders().getFirst("X-Internal-Service-Token"));
        assertNotNull(forwardedExchange.get().getRequest().getHeaders().getFirst("Authorization"));
    }
}
