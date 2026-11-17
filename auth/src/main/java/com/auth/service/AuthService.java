package com.auth.service;

import com.auth.dto.LoginRequest;
import com.auth.dto.RegisterRequest;
import com.auth.dto.UserResponse;
import com.auth.dto.AuthResponse;
import com.auth.entity.User;
import com.auth.exception.DuplicateEmailException;
import com.auth.exception.InvalidCredentialsException;
import com.auth.exception.PasswordTooLongException;
import com.auth.exception.UserNotFoundException;
import com.auth.repository.UserRepository;
import com.auth.security.AuthPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration details are required.");
        }

        String email = normalizeEmail(request.email());
        String password = request.password();
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new PasswordTooLongException();
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }

        User user = new User(request.name().trim(), email, passwordEncoder.encode(password));
        return toUserResponse(userRepository.save(user));
    }

    @Transactional
    public AuthTokenResult authenticate(LoginRequest request) {
        if (request == null) {
            throw new InvalidCredentialsException();
        }

        String password = request.password();
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidCredentialsException();
        }

        try {
            // Spring loads the account from MySQL and verifies the submitted password with BCrypt.
            var authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            normalizeEmail(request.email()), password));
            AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
            User user = userRepository.findById(principal.getUserId())
                    .orElseThrow(InvalidCredentialsException::new);
            String refreshToken = refreshTokenService.issue(user);
            return new AuthTokenResult(createAuthResponse(user), refreshToken);
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }
    }

    public AuthTokenResult refresh(String rawRefreshToken) {
        RefreshTokenGrant grant = refreshTokenService.rotate(rawRefreshToken);
        return new AuthTokenResult(createAuthResponse(grant.user()), grant.refreshToken());
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(UserNotFoundException::new);
        return toUserResponse(user);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidCredentialsException();
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt());
    }

    private AuthResponse createAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                "Bearer",
                jwtService.getAccessTokenTtlSeconds(),
                toUserResponse(user));
    }
}
