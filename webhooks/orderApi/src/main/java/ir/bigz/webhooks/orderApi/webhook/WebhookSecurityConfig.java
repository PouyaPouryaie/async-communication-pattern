package ir.bigz.webhooks.orderApi.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Binds {@link HmacVerificationFilter} exclusively to the payment webhook
 * endpoint so the public order API is not affected by it.
 */
@Configuration
public class WebhookSecurityConfig {

    @Bean
    public FilterRegistrationBean<HmacVerificationFilter> hmacVerificationFilter(
            @Value("${app.webhook.secret}") String webhookSecret) {
        FilterRegistrationBean<HmacVerificationFilter> registration =
                new FilterRegistrationBean<>(new HmacVerificationFilter(webhookSecret));
        registration.addUrlPatterns("/api/webhooks/payments");
        return registration;
    }
}
