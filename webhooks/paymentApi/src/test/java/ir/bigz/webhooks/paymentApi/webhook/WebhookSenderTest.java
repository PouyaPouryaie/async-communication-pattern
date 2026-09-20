package ir.bigz.webhooks.paymentApi.webhook;

import ir.bigz.webhooks.paymentApi.common.HmacSignatureUtil;
import ir.bigz.webhooks.paymentApi.domain.MerchantRegistration;
import ir.bigz.webhooks.paymentApi.domain.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WebhookSenderTest {

    @Test
    void deliverSignsRequestBodyWithMerchantSecret() {
        ObjectMapper objectMapper = new ObjectMapper();
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        WebhookSender webhookSender = new WebhookSender(restClientBuilder, objectMapper);

        MerchantRegistration merchant = new MerchantRegistration();
        merchant.setId(UUID.randomUUID());
        merchant.setWebhookUrl("https://store.example/api/webhooks/payments");
        merchant.setSecret("shared-secret");

        WebhookPayload payload = new WebhookPayload(
                UUID.randomUUID(), "order-1", PaymentStatus.SUCCEEDED, new BigDecimal("49.99"), "USD", Instant.now());
        String expectedBody = objectMapper.writeValueAsString(payload);
        String expectedSignature = HmacSignatureUtil.sign(expectedBody, "shared-secret");

        server.expect(requestTo("https://store.example/api/webhooks/payments"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(WebhookSender.SIGNATURE_HEADER, expectedSignature))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        webhookSender.deliver(merchant, payload);

        server.verify();
    }
}
