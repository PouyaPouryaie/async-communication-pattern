package ir.bigz.webhooks.orderApi.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Registers this store's webhook URL and shared secret with the payment
 * application on every startup. Registration is an upsert on the payment
 * application side (keyed by webhook URL), so repeating it on restart is
 * safe. Retries with a fixed delay because the payment application may not
 * be ready yet when this store starts (e.g. in docker-compose).
 */
@Component
public class RegistrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RegistrationRunner.class);

    private final RestClient restClient;
    private final MerchantRegistrationHolder merchantRegistrationHolder;
    private final String webhookUrl;
    private final String webhookSecret;
    private final int maxAttempts;
    private final long retryDelayMillis;

    public RegistrationRunner(RestClient.Builder restClientBuilder,
                               MerchantRegistrationHolder merchantRegistrationHolder,
                               @Value("${app.payment-api.base-url}") String paymentApiBaseUrl,
                               @Value("${app.public-base-url}") String publicBaseUrl,
                               @Value("${app.webhook.secret}") String webhookSecret,
                               @Value("${app.registration.max-attempts:10}") int maxAttempts,
                               @Value("${app.registration.retry-delay-seconds:3}") long retryDelaySeconds) {
        this.restClient = restClientBuilder.baseUrl(paymentApiBaseUrl).build();
        this.merchantRegistrationHolder = merchantRegistrationHolder;
        this.webhookUrl = publicBaseUrl + "/api/webhooks/payments";
        this.webhookSecret = webhookSecret;
        this.maxAttempts = maxAttempts;
        this.retryDelayMillis = retryDelaySeconds * 1000;
    }

    @Override
    public void run(ApplicationArguments args) throws InterruptedException {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                RegisterResponse response = restClient.post()
                        .uri("/api/registry/register")
                        .body(new RegisterRequest(webhookUrl, webhookSecret))
                        .retrieve()
                        .body(RegisterResponse.class);
                merchantRegistrationHolder.set(response.merchantId());
                log.info("Registered with payment application as merchant {}", response.merchantId());
                return;
            } catch (RestClientException e) {
                log.warn("Registration attempt {}/{} with payment application failed: {}",
                        attempt, maxAttempts, e.getMessage());
                if (attempt == maxAttempts) {
                    log.error("Giving up registering with the payment application after {} attempts", maxAttempts);
                    return;
                }
                Thread.sleep(retryDelayMillis);
            }
        }
    }

    private record RegisterRequest(String webhookUrl, String secret) {
    }

    private record RegisterResponse(String merchantId) {
    }
}
