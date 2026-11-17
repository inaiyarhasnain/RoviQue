package com.auth.service;

import com.auth.entity.User;

public record RefreshTokenGrant(User user, String refreshToken) {
}
