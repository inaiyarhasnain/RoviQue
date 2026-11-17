package com.auth.controller;

import com.auth.config.SecurityConfig;
import com.auth.dto.AuthResponse;
import com.auth.dto.UserResponse;
import com.auth.entity.Role;
import com.auth.service.AuthService;
import com.auth.service.AuthTokenResult;
import com.auth.service.JwtService;
import com.auth.entity.User;
import com.auth.security.AuthPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.Cookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "auth.cors.allowed-origins=http://localhost:3000",
        "auth.refresh-cookie-secure=false"
})
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    void registrationAndLoginArePublic() throws Exception {
        UserResponse user = new UserResponse(
                7L, "Example User", "user@example.com", Role.USER, LocalDateTime.now());
        when(authService.register(any())).thenReturn(user);
        when(authService.authenticate(any())).thenReturn(new AuthTokenResult(
                new AuthResponse("access-token", "Bearer", 900, user), "raw-refresh-token"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Example User",
                                  "email": "user@example.com",
                                  "password": "correct-horse"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "correct-horse"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("ROVIQUE_REFRESH_TOKEN=raw-refresh-token")));
    }

    @Test
    void refreshRotatesHttpOnlyCookieWithoutRequiringAnAccessToken() throws Exception {
        UserResponse user = new UserResponse(
                7L, "Example User", "user@example.com", Role.USER, LocalDateTime.now());
        when(authService.refresh("old-refresh-token")).thenReturn(new AuthTokenResult(
                new AuthResponse("new-access-token", "Bearer", 900, user), "new-refresh-token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("ROVIQUE_REFRESH_TOKEN", "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("ROVIQUE_REFRESH_TOKEN=new-refresh-token"),
                                org.hamcrest.Matchers.containsString("HttpOnly"),
                                org.hamcrest.Matchers.containsString("SameSite=Strict"))));
    }

    @Test
    void logoutRevokesAndClearsRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("ROVIQUE_REFRESH_TOKEN", "old-refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("Max-Age=0")));
    }

    @Test
    void currentUserRequiresAnAuthenticatedJwt() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void currentUserAcceptsAnAuthenticatedJwt() throws Exception {
        UserResponse user = new UserResponse(
                7L, "Example User", "user@example.com", Role.USER, LocalDateTime.now());
        when(authService.getCurrentUser(anyString())).thenReturn(user);

        mockMvc.perform(get("/api/v1/auth/me")
                        .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                                new AuthPrincipal(new User("Example User", "user@example.com", "hashed")),
                                null,
                                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "ROLE_USER"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }
}
