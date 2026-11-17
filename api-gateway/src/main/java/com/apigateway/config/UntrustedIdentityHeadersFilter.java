package com.apigateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class UntrustedIdentityHeadersFilter implements GlobalFilter, Ordered {

    private static final List<String> IDENTITY_HEADERS = List.of(
            "X-User-Id",
            "X-User-Email",
            "X-User-Roles",
            "X-Auth-User",
            "X-Auth-Roles",
            "X-Roles",
            "X-Authenticated-User",
            "X-Authenticated-Roles",
            "X-Forwarded-User",
            "X-Internal-Service-Token",
            "Remote-User");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> IDENTITY_HEADERS.forEach(headers::remove))
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
