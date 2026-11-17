package com.auth.service;

import com.auth.dto.AuthResponse;

public record AuthTokenResult(AuthResponse response, String refreshToken) {
}
