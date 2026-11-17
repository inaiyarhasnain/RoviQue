package com.apigateway.config;

import com.apigateway.security.GatewayJwtAuthenticationConverter;
import com.apigateway.security.GatewayJwtAuthenticationManager;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Configuration
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain gatewaySecurityWebFilterChain(
            ServerHttpSecurity http,
            GatewayJwtAuthenticationManager authenticationManager,
            GatewayJwtAuthenticationConverter authenticationConverter,
            CorsConfigurationSource gatewayCorsConfigurationSource,
            ServerAuthenticationEntryPoint gatewayAuthenticationEntryPoint) {
        AuthenticationWebFilter jwtFilter = new AuthenticationWebFilter(authenticationManager);
        jwtFilter.setServerAuthenticationConverter(authenticationConverter);
        jwtFilter.setAuthenticationFailureHandler(
                new ServerAuthenticationEntryPointFailureHandler(gatewayAuthenticationEntryPoint));

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(gatewayCorsConfigurationSource))
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(gatewayAuthenticationEntryPoint))
                .authorizeExchange(authorize -> authorize
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/payment/webhook").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/product/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/product/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/v1/product/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/v1/product/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/product/**").hasRole("ADMIN")
                        .pathMatchers("/api/v1/cart/**", "/api/v1/order/**", "/api/v1/payment/**")
                        .authenticated()
                        .pathMatchers("/api/v1/auth/me").authenticated()
                        .anyExchange().authenticated())
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        return http.build();
    }

    @Bean
    SecretKey jwtSigningKey(@Value("${security.jwt.secret}") String encodedSecret) {
        try {
            byte[] secret = Base64.getDecoder().decode(encodedSecret);
            if (secret.length < 32) {
                throw new IllegalStateException("AUTH_JWT_SECRET must contain at least 256 bits.");
            }
            return Keys.hmacShaKeyFor(secret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("AUTH_JWT_SECRET must be a Base64-encoded key.", exception);
        }
    }

    @Bean
    ServerAuthenticationEntryPoint gatewayAuthenticationEntryPoint() {
        return (exchange, exception) -> {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        };
    }

    @Bean
    public CorsConfigurationSource gatewayCorsConfigurationSource(
            @Value("${gateway.cors.allowed-origins}") String configuredOrigins) {
        List<String> origins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        if (origins.isEmpty() || origins.contains("*")) {
            throw new IllegalStateException("Configure explicit Gateway CORS origins; wildcard origins are not allowed.");
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "X-CART-ID", "Stripe-Signature"));
        configuration.setExposedHeaders(List.of("X-CART-ID"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
