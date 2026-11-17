package com.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

// Start the full Spring application context to check that application wiring works.
@SpringBootTest(properties = "security.internal.payment-token=test-internal-token")
@AutoConfigureMockMvc
class OrderApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    // Tell JUnit to run this method as a test.
    @Test
    void contextLoads() {
    }

    @Test
    void paymentStatusRequiresInternalPaymentServiceToken() throws Exception {
        int withoutServiceToken = mockMvc.perform(put("/api/v1/order/1/payment-status")
                .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                        "payment-user", null, java.util.List.of())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentStatus\":\"PAID\"}"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertEquals(403, withoutServiceToken);

        int withServiceToken = mockMvc.perform(put("/api/v1/order/1/payment-status")
                .with(authentication(UsernamePasswordAuthenticationToken.authenticated(
                        "payment-user", null, java.util.List.of())))
                        .header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentStatus\":\"PAID\"}"))
                .andReturn()
                .getResponse()
                .getStatus();
        assertNotEquals(403, withServiceToken);
    }
}
