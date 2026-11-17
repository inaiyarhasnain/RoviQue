package com.apigateway.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;
import static org.springframework.test.web.reactive.server.WebTestClient.bindToApplicationContext;

@SpringBootTest(properties = {
        "security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "security.jwt.issuer=http://localhost:8085",
        "security.jwt.audience=rovique-api"
})
class GatewaySecurityTest {

    @Autowired
    private ApplicationContext applicationContext;

    private static final SecretKey SIGNING_KEY =
            Keys.hmacShaKeyFor(Base64.getDecoder().decode("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="));
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = bindToApplicationContext(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void cartRoutesRequireAuthentication() {
        webTestClient.get()
                .uri("/api/v1/cart/test-cart")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void userCannotCallAdminProductMutationRoutes() {
        webTestClient.post()
                .uri("/api/v1/product/products")
                .header("Authorization", "Bearer " + token("user@example.com", 42L, "USER"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void productBrowsingIsPublicAtTheGateway() {
        webTestClient.get()
                .uri("/api/v1/product/list/categories")
                .exchange()
                .expectStatus().value(status -> {
                    assertNotEquals(401, status);
                    assertNotEquals(403, status);
                });
    }

    @Test
    void adminCanPassGatewayProductMutationAuthorization() {
        webTestClient.post()
                .uri("/api/v1/product/products")
                .header("Authorization", "Bearer " + token("admin@example.com", 1L, "ADMIN"))
                .exchange()
                .expectStatus().value(status -> {
                    assertNotEquals(401, status);
                    assertNotEquals(403, status);
                });
    }

    @Test
    void rejectsTamperedAndExpiredTokens() {
        String valid = token("user@example.com", 42L, "USER");
        int signatureStart = valid.lastIndexOf('.') + 1;
        char replacement = valid.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = valid.substring(0, signatureStart)
                + replacement + valid.substring(signatureStart + 1);

        webTestClient.get()
                .uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + tampered)
                .exchange()
                .expectStatus().isUnauthorized();
        webTestClient.get()
                .uri("/api/v1/auth/me")
                .header("Authorization", "Bearer " + expiredToken())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private String token(String email, Long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("http://localhost:8085")
                .subject(email)
                .audience().add("rovique-api").and()
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plusSeconds(900)))
                .claim("userId", userId)
                .claim("roles", List.of(role))
                .signWith(SIGNING_KEY, Jwts.SIG.HS256)
                .compact();
    }

    private String expiredToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("http://localhost:8085")
                .subject("user@example.com")
                .audience().add("rovique-api").and()
                .issuedAt(java.util.Date.from(now.minusSeconds(120)))
                .expiration(java.util.Date.from(now.minusSeconds(60)))
                .claim("userId", 42L)
                .claim("roles", List.of("USER"))
                .signWith(SIGNING_KEY, Jwts.SIG.HS256)
                .compact();
    }
}
