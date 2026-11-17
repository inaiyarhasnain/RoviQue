package com.auth.config;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtConfigTest {

    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    void loadsAValidBase64HmacKey() {
        assertNotNull(jwtConfig.jwtSigningKey(Base64.getEncoder().encodeToString(new byte[32])));
    }

    @Test
    void rejectsWeakHmacKeys() {
        assertThrows(IllegalStateException.class,
                () -> jwtConfig.jwtSigningKey(Base64.getEncoder().encodeToString(new byte[16])));
    }
}
