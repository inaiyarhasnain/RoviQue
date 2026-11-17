package com.payment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Configure browser pages served by the payment application.
@Configuration
public class PaymentPageConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward Stripe's success redirect to the static page that checks payment status.
        registry.addViewController("/payment/success")
                .setViewName("forward:/payment/success.html");

        // Forward Stripe's cancel redirect to a simple cancellation page.
        registry.addViewController("/payment/cancel")
                .setViewName("forward:/payment/cancel.html");
    }
}
