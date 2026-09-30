package ir.bigz.webhooks.paymentApi.webhook;

import ir.bigz.webhooks.paymentApi.common.HmacSignatureUtil;
import ir.bigz.webhooks.paymentApi.domain.MerchantRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

@Component
public class WebhookSender {

    private static final Logger log = LoggerFactory.getLogger(WebhookSender.class);
    static final String SIGNATURE_HEADER = "X-Webhook-Signature";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public WebhookSender(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Delivers the webhook, signing the exact JSON bytes sent so the merchant
     * can independently recompute the same signature. Retries with an
     * exponential backoff on any transport failure or non-2xx response; after
     * all attempts are exhausted, {@link #recover} logs the permanent failure.
     */
    @Retryable(
            retryFor = RestClientException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 30000))
    public void deliver(MerchantRegistration merchant, WebhookPayload payload) {
        String body = objectMapper.writeValueAsString(payload);
        String signature = HmacSignatureUtil.sign(body, merchant.getSecret());

        restClient.post()
                .uri(merchant.getWebhookUrl())
                .header(SIGNATURE_HEADER, signature)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        log.info("Delivered webhook for payment {} to {}", payload.paymentId(), merchant.getWebhookUrl());
    }

    @Recover
    public void recover(RestClientException exception, MerchantRegistration merchant, WebhookPayload payload) {
        log.error("Giving up delivering webhook for payment {} to {} after retries: {}",
                payload.paymentId(), merchant.getWebhookUrl(), exception.getMessage());
    }
}
