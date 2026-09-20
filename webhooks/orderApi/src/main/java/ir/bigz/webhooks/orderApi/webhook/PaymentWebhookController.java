package ir.bigz.webhooks.orderApi.webhook;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives payment outcome webhooks from the payment application. Every
 * request to this path is first verified by {@link HmacVerificationFilter}
 * before reaching this controller.
 */
@RestController
@RequestMapping("/api/webhooks/payments")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    public PaymentWebhookController(PaymentWebhookService paymentWebhookService) {
        this.paymentWebhookService = paymentWebhookService;
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody PaymentWebhookPayload payload) {
        paymentWebhookService.processWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
