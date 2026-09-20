package ir.bigz.webhooks.orderApi.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * Calls the payment application to start a simulated payment for an order.
 * The call returns immediately with a {@code PENDING} status; the actual
 * outcome arrives later via the webhook this store registered.
 */
@Component
public class PaymentClient {

    private final RestClient restClient;
    private final MerchantRegistrationHolder merchantRegistrationHolder;

    public PaymentClient(RestClient.Builder restClientBuilder,
                          MerchantRegistrationHolder merchantRegistrationHolder,
                          @Value("${app.payment-api.base-url}") String paymentApiBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(paymentApiBaseUrl).build();
        this.merchantRegistrationHolder = merchantRegistrationHolder;
    }

    public PaymentInitiationResult initiatePayment(String orderId, BigDecimal amount, String currency) {
        InitiateRequest request = new InitiateRequest(merchantRegistrationHolder.get(), orderId, amount, currency);
        InitiateResponse response = restClient.post()
                .uri("/api/payments/initiate")
                .body(request)
                .retrieve()
                .body(InitiateResponse.class);
        return new PaymentInitiationResult(response.paymentId(), response.status());
    }

    private record InitiateRequest(String merchantId, String orderId, BigDecimal amount, String currency) {
    }

    private record InitiateResponse(String paymentId, String status) {
    }
}
