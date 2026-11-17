package com.apigateway.security;

import java.util.List;

public record GatewayPrincipal(Long userId, String email, List<String> roles) {
}
