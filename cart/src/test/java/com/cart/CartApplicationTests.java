package com.cart;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// Start the full Spring application context to check that application wiring works.
@SpringBootTest
@AutoConfigureMockMvc
class CartApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    // Tell JUnit to run this method as a test.
    @Test
    void contextLoads() {
    }

    @Test
    void cartEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/cart/unknown-cart"))
                .andExpect(status().isUnauthorized());
    }
}
