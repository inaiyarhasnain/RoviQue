package com.auth.service;

import com.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private static final String AUDIENCE = "rovique-api";

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Clock clock;

    public JwtService(
            SecretKey signingKey,
            @Value("${auth.jwt.issuer}") String issuer,
            @Value("${auth.jwt.access-token-ttl}") Duration accessTokenTtl,
            Clock clock) {
        this.signingKey = signingKey;
        this.issuer = issuer;
        this.accessTokenTtl = accessTokenTtl;
        this.clock = clock;
        if (accessTokenTtl.isZero() || accessTokenTtl.isNegative() || accessTokenTtl.toSeconds() == 0) {
            throw new IllegalStateException("JWT access-token TTL must be positive.");
        }
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    public String generateAccessToken(User user) {
        if (user == null || user.getId() == null || user.getRole() == null) {
            throw new IllegalArgumentException("A persisted user with a role is required to issue a JWT.");
        }

        Date issuedAt = Date.from(clock.instant());
        // Sign a short-lived token containing only the identity and role needed by the services.
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getEmail())
                .audience().add(AUDIENCE).and()
                .issuedAt(issuedAt)
                .expiration(Date.from(clock.instant().plus(accessTokenTtl)))
                .claim("userId", user.getId())
                .claim("roles", List.of(user.getRole().name()))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("JWT is required.");
        }
        // Signature, issuer, audience, and expiration are checked before claims are trusted.
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .requireAudience(AUDIENCE)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            parseClaims(token);
            return false;
        } catch (ExpiredJwtException exception) {
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new JwtException("JWT is invalid.", exception);
        }
    }

    public String extractEmail(String token) {
        String subject = parseClaims(token).getSubject();
        if (subject == null || subject.isBlank()) {
            throw new JwtException("JWT does not contain an email subject.");
        }
        return subject;
    }

    public Long extractUserId(String token) {
        Object claim = parseClaims(token).get("userId");
        if (claim instanceof Number number && number.longValue() > 0) {
            return number.longValue();
        }
        throw new JwtException("JWT does not contain a valid user ID.");
    }

    public List<String> extractRoles(String token) {
        Object claim = parseClaims(token).get("roles");
        if (claim instanceof List<?> roles
                && !roles.isEmpty()
                && roles.stream().allMatch(String.class::isInstance)) {
            return roles.stream().map(String.class::cast).toList();
        }
        throw new JwtException("JWT does not contain roles.");
    }
}
