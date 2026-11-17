package com.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class GatewayJwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final GatewayJwtTokenService tokenService;

    public GatewayJwtAuthenticationManager(GatewayJwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (!(authentication instanceof GatewayJwtAuthenticationToken token)) {
            return Mono.empty();
        }

        try {
            // Validate the signed token here so the Gateway authorizes only verified claims.
            Claims claims = tokenService.parseClaims((String) token.getCredentials());
            Object idClaim = claims.get("userId");
            Object roleClaim = claims.get("roles");
            if (!(idClaim instanceof Number id) || id.longValue() <= 0
                    || !(roleClaim instanceof List<?> rawRoles)
                    || rawRoles.isEmpty()
                    || !rawRoles.stream().allMatch(String.class::isInstance)
                    || claims.getSubject() == null || claims.getSubject().isBlank()) {
                throw new JwtException("JWT identity claims are invalid.");
            }

            List<String> roles = rawRoles.stream().map(String.class::cast).toList();
            GatewayPrincipal principal = new GatewayPrincipal(id.longValue(), claims.getSubject(), roles);
            var authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            return Mono.just(UsernamePasswordAuthenticationToken.authenticated(
                    principal, token.getCredentials(), authorities));
        } catch (JwtException | IllegalArgumentException exception) {
            return Mono.error(new BadCredentialsException("Invalid or expired bearer token.", exception));
        }
    }
}
