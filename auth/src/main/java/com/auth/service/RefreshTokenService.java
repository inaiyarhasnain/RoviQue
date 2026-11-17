package com.auth.service;

import com.auth.entity.RefreshToken;
import com.auth.entity.User;
import com.auth.exception.InvalidRefreshTokenException;
import com.auth.exception.RefreshTokenReuseException;
import com.auth.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private static final int RANDOM_TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration tokenTtl;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${auth.refresh-token-ttl}") Duration tokenTtl,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenTtl = tokenTtl;
        this.clock = clock;
        if (tokenTtl.isNegative() || tokenTtl.isZero()) {
            throw new IllegalStateException("Refresh-token TTL must be positive.");
        }
    }

    @Transactional
    public String issue(User user) {
        String rawToken = generateToken();
        refreshTokenRepository.save(newToken(user, rawToken, clock.instant()));
        return rawToken;
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RefreshTokenGrant rotate(String rawToken) {
        if (!isValidFormat(rawToken)) {
            throw new InvalidRefreshTokenException();
        }

        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = clock.instant();

        if (current.getRevokedAt() != null) {
            refreshTokenRepository.revokeAllActiveByUserId(current.getUser().getId(), now);
            throw new RefreshTokenReuseException();
        }
        if (current.getExpiresAt() == null || !current.getExpiresAt().isAfter(now)) {
            current.setRevokedAt(now);
            throw new InvalidRefreshTokenException();
        }

        String replacement = generateToken();
        String replacementHash = hash(replacement);
        current.setRevokedAt(now);
        current.setReplacedByTokenHash(replacementHash);
        refreshTokenRepository.save(current);
        refreshTokenRepository.save(newToken(current.getUser(), replacement, now));
        return new RefreshTokenGrant(current.getUser(), replacement);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (!isValidFormat(rawToken)) {
            return;
        }
        refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(clock.instant());
                refreshTokenRepository.save(token);
            }
        });
    }

    private RefreshToken newToken(User user, String rawToken, Instant now) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setCreatedAt(now);
        refreshToken.setExpiresAt(now.plus(tokenTtl));
        return refreshToken;
    }

    private String generateToken() {
        byte[] bytes = new byte[RANDOM_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean isValidFormat(String rawToken) {
        return rawToken != null && rawToken.matches("[A-Za-z0-9_-]{43}");
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required to hash refresh tokens.", exception);
        }
    }
}
