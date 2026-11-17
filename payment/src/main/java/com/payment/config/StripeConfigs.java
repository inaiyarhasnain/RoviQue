package com.payment.config;

import com.stripe.StripeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Tell Spring this class defines configuration for Stripe.
@Configuration
public class StripeConfigs{

    // Read the Stripe secret key from Spring configuration without hard-coding it.
    @Value("${stripe.secret.key}")
    private String secretKey;

    // Create a Stripe client that can be injected into the payment service.
    @Bean
    public StripeClient stripeClient() {
        return new StripeClient(secretKey);
    }
}
