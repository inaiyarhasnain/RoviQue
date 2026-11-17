package com.auth.security;

import com.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthPrincipal principal;
        try {
            // Verify signature and claims, then reload the account to reject deleted or changed users.
            Claims claims = jwtService.parseClaims(authorization.substring(7));
            UserDetails loadedUser = userDetailsService.loadUserByUsername(claims.getSubject());
            if (!(loadedUser instanceof AuthPrincipal authPrincipal)
                    || !authPrincipal.getUserId().equals(readUserId(claims))
                    || !List.of(authPrincipal.getRole().name()).equals(readRoles(claims))) {
                reject(response);
                return;
            }
            principal = authPrincipal;
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException exception) {
            SecurityContextHolder.clearContext();
            reject(response);
            return;
        }

        // Make the verified database principal available to controllers and authorization rules.
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private Long readUserId(Claims claims) {
        Object value = claims.get("userId");
        if (value instanceof Number number && number.longValue() > 0) {
            return number.longValue();
        }
        throw new JwtException("JWT user ID is invalid.");
    }

    private List<String> readRoles(Claims claims) {
        Object value = claims.get("roles");
        if (value instanceof List<?> roles
                && !roles.isEmpty()
                && roles.stream().allMatch(String.class::isInstance)) {
            return roles.stream().map(String.class::cast).toList();
        }
        throw new JwtException("JWT roles are invalid.");
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired bearer token.");
    }
}
