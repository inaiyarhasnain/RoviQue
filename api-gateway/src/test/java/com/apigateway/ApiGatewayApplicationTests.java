package com.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
class ApiGatewayApplicationTests {

    @Value("${spring.cloud.gateway.server.webflux.routes[4].uri}")
    private String authRouteUri;

    @Value("${spring.cloud.gateway.server.webflux.routes[4].predicates[0]}")
    private String authRoutePath;

    @Test
    void contextLoads() {
    }

    @Test
    void authRouteUsesEurekaDiscovery() {
        assertEquals("lb://auth", authRouteUri);
        assertEquals("Path=/api/v1/auth/**", authRoutePath);
    }
}
