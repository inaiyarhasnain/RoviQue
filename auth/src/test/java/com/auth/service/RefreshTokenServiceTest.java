package com.auth.service;

import com.auth.entity.RefreshToken;
import com.auth.entity.User;
import com.auth.exception.InvalidRefreshTokenException;
import com.auth.exception.RefreshTokenReuseException;
import com.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void storesOnlyTokenHashAndSetsExpiration() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RefreshTokenService service = service(repository);

        String rawToken = service.issue(user());

        var savedToken = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(savedToken.capture());
        assertEquals(43, rawToken.length());
        assertNotEquals(rawToken, savedToken.getValue().getTokenHash());
        assertEquals(64, savedToken.getValue().getTokenHash().length());
        assertEquals(NOW.plus(Duration.ofDays(30)), savedToken.getValue().getExpiresAt());
    }

    @Test
    void rotatesTokenAndLinksTheRevokedTokenToItsReplacement() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RefreshTokenService service = service(repository);
        User user = user();
        String rawToken = service.issue(user);
        var savedToken = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(savedToken.capture());
        RefreshToken current = savedToken.getValue();
        when(repository.findByTokenHashForUpdate(current.getTokenHash())).thenReturn(Optional.of(current));

        RefreshTokenGrant grant = service.rotate(rawToken);

        assertEquals(user, grant.user());
        assertNotEquals(rawToken, grant.refreshToken());
        assertEquals(NOW, current.getRevokedAt());
        assertEquals(64, current.getReplacedByTokenHash().length());
        assertTrue(current.getExpiresAt().isAfter(NOW));
        verify(repository, org.mockito.Mockito.times(3)).save(any(RefreshToken.class));
    }

    @Test
    void reuseOfRevokedTokenRevokesEveryActiveSession() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        User user = user();
        String rawToken = service(repository).issue(user);
        var savedToken = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(savedToken.capture());
        RefreshToken revoked = savedToken.getValue();
        revoked.setRevokedAt(NOW.minusSeconds(1));
        when(repository.findByTokenHashForUpdate(revoked.getTokenHash())).thenReturn(Optional.of(revoked));

        assertThrows(RefreshTokenReuseException.class, () -> service(repository).rotate(rawToken));

        verify(repository).revokeAllActiveByUserId(7L, NOW);
    }

    @Test
    void expiredTokenIsRevokedAndRejected() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        User user = user();
        String rawToken = service(repository).issue(user);
        var savedToken = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(savedToken.capture());
        RefreshToken expired = savedToken.getValue();
        expired.setExpiresAt(NOW.minusSeconds(1));
        when(repository.findByTokenHashForUpdate(expired.getTokenHash())).thenReturn(Optional.of(expired));

        assertThrows(InvalidRefreshTokenException.class, () -> service(repository).rotate(rawToken));

        assertEquals(NOW, expired.getRevokedAt());
    }

    private RefreshTokenService service(RefreshTokenRepository repository) {
        return new RefreshTokenService(
                repository,
                Duration.ofDays(30),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private User user() {
        User user = new User("Test User", "user@example.com", "hashed-password");
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }
}
