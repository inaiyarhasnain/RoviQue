package com.auth.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.util.Base64;

@Configuration
public class JwtConfig {

    @Bean
    SecretKey jwtSigningKey(@Value("${auth.jwt.secret}") String encodedSecret) {
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
    java.time.Clock jwtClock() {
        return java.time.Clock.systemUTC();
    }
}
