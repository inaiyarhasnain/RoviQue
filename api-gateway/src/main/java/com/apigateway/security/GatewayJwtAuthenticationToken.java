package com.apigateway.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;

public class GatewayJwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String token;

    public GatewayJwtAuthenticationToken(String token) {
        super(List.of());
        this.token = token;
        setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }
}
