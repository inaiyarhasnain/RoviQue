package com.order.config;

import com.order.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class DownstreamSecurityConfig {

    @Bean
    SecurityFilterChain orderSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            AuthorizationManager<RequestAuthorizationContext> paymentStatusAuthorizationManager)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(401, "Authentication is required.")))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.PUT, "/api/v1/order/*/payment-status")
                        .access(paymentStatusAuthorizationManager)
                        .requestMatchers("/api/v1/internal/**")
                        .access(paymentStatusAuthorizationManager)
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    AuthorizationManager<RequestAuthorizationContext> paymentStatusAuthorizationManager(
            @Value("${security.internal.payment-token:}") String configuredToken) {
        return (authentication, context) -> {
            String suppliedToken = context.getRequest().getHeader("X-Internal-Service-Token");
            boolean authorized = configuredToken != null
                    && !configuredToken.isBlank()
                    && suppliedToken != null
                    && MessageDigest.isEqual(
                            configuredToken.getBytes(StandardCharsets.UTF_8),
                            suppliedToken.getBytes(StandardCharsets.UTF_8));
            return new AuthorizationDecision(authorized);
        };
    }
}
