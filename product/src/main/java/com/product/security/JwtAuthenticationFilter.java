package com.product.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        JwtPrincipal principal;
        List<SimpleGrantedAuthority> authorities;
        try {
            // Validate the signature and standard claims before creating a Spring Security principal.
            Claims claims = jwtTokenService.parseClaims(header.substring(7));
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
            principal = new JwtPrincipal(id.longValue(), claims.getSubject(), roles);
            authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired bearer token.");
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities));
        filterChain.doFilter(request, response);
    }
}
