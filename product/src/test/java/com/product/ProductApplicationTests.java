package com.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// Start the full Spring application context to check that application wiring works.
@SpringBootTest
@AutoConfigureMockMvc
class ProductApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    // Tell JUnit to run this method as a test.
    @Test
    void contextLoads() {
    }

    @Test
    void publicCategoryBrowsingDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/product/list/categories"))
                .andExpect(status().isOk());
    }

    @Test
    void catalogWritesRequireAdministratorRole() throws Exception {
        mockMvc.perform(post("/api/v1/product/categories")
                .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                        "user", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/product/categories")
                .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                        "admin", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
