package com.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// Start Spring with a test-only Stripe key so no real secret is needed for this context test.
@SpringBootTest(properties = {
        "stripe.secret.key=sk_test_placeholder",
        "stripe.webhook.secret=whsec_test_placeholder",
        "security.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "security.jwt.issuer=http://localhost:8085",
        "security.jwt.audience=rovique-api"
})
@AutoConfigureMockMvc
class PaymentApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    // Tell JUnit to verify that the application context starts.
    @Test
    void contextLoads() {
    }

    @Test
    void paymentStatusRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/payment/orders/1/status"))
                .andExpect(status().isUnauthorized());
    }
}
