package com.auth.service;

import com.auth.entity.User;
import com.auth.entity.Role;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String ISSUER = "http://localhost:8085";
    private static final SecretKey SIGNING_KEY = Keys.hmacShaKeyFor(new byte[32]);

    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final JwtService jwtService = new JwtService(
            SIGNING_KEY,
            ISSUER,
            Duration.ofMinutes(15),
            Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void generatesAndValidatesTokenWithUserClaims() {
        User user = new User("Example User", "user@example.com", "hashed-password");
        user.setRole(Role.USER);
        ReflectionTestUtils.setField(user, "id", 42L);

        String token = jwtService.generateAccessToken(user);

        assertTrue(jwtService.validateToken(token));
        assertFalse(jwtService.isTokenExpired(token));
        assertEquals("user@example.com", jwtService.extractEmail(token));
        assertEquals(42L, jwtService.extractUserId(token));
        assertEquals(List.of("USER"), jwtService.extractRoles(token));
    }

    @Test
    void identifiesExpiredSignedTokens() {
        String token = Jwts.builder()
                .issuer(ISSUER)
                .subject("user@example.com")
                .audience().add("rovique-api").and()
                .issuedAt(Date.from(now.minusSeconds(180)))
                .expiration(Date.from(now.minusSeconds(120)))
                .claim("userId", 42L)
                .claim("roles", List.of("USER"))
                .signWith(SIGNING_KEY, Jwts.SIG.HS256)
                .compact();

        assertFalse(jwtService.validateToken(token));
        assertTrue(jwtService.isTokenExpired(token));
    }

    @Test
    void rejectsTamperedToken() {
        User user = new User("Example User", "user@example.com", "hashed-password");
        user.setRole(Role.USER);
        ReflectionTestUtils.setField(user, "id", 42L);
        String token = jwtService.generateAccessToken(user);
        String tamperedToken = token.substring(0, token.lastIndexOf('.') + 1) + "AAAA";

        assertFalse(jwtService.validateToken(tamperedToken));
        assertThrows(JwtException.class, () -> jwtService.isTokenExpired(tamperedToken));
    }
}
