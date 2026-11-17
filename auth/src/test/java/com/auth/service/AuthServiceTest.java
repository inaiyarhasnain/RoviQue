package com.auth.service;

import com.auth.dto.LoginRequest;
import com.auth.dto.RegisterRequest;
import com.auth.entity.Role;
import com.auth.entity.User;
import com.auth.exception.DuplicateEmailException;
import com.auth.exception.InvalidCredentialsException;
import com.auth.repository.UserRepository;
import com.auth.security.AuthPrincipal;
import com.auth.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        authService = new AuthService(
                userRepository, passwordEncoder, jwtService, refreshTokenService, authenticationManager);
    }

    @Test
    void registerStoresHashedPasswordAndDefaultsToUserRole() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.register(
                new RegisterRequest("Example User", " User@Example.com ", "correct-horse"));

        var savedUser = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("user@example.com", response.email());
        assertEquals(Role.USER, response.role());
        assertEquals(Role.USER, savedUser.getValue().getRole());
        assertTrue(passwordEncoder.matches("correct-horse", savedUser.getValue().getPassword()));
        assertNotEquals("correct-horse", savedUser.getValue().getPassword());
    }

    @Test
    void registerRejectsAnExistingEmail() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(
                new RegisterRequest("Example User", "user@example.com", "correct-horse")));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void authenticateReturnsSafeUserDetailsForValidPassword() {
        User user = new User("Example User", "user@example.com", passwordEncoder.encode("correct-horse"));
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 7L);
        when(authenticationManager.authenticate(any())).thenReturn(
                UsernamePasswordAuthenticationToken.authenticated(
                        new AuthPrincipal(user), null, new AuthPrincipal(user).getAuthorities()));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        when(jwtService.generateAccessToken(user)).thenReturn("signed-access-token");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(refreshTokenService.issue(user)).thenReturn("raw-refresh-token");

        var response = authService.authenticate(new LoginRequest("USER@example.com", "correct-horse"));

        assertEquals("signed-access-token", response.response().accessToken());
        assertEquals("Bearer", response.response().tokenType());
        assertEquals(900L, response.response().expiresIn());
        assertEquals("user@example.com", response.response().user().email());
        assertEquals(Role.USER, response.response().user().role());
        assertEquals("raw-refresh-token", response.refreshToken());
    }

    @Test
    void authenticateUsesGenericErrorForUnknownEmail() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> authService.authenticate(
                new LoginRequest("unknown@example.com", "wrong-password")));
    }
}
