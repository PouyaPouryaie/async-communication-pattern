package ir.bigz.webhooks.orderApi.webhook;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The JSON body delivered by the payment application once a payment reaches
 * a terminal state. Matches the wire contract the payment application signs
 * with HMAC-SHA256 — see {@code WebhookPayload} on the payment application side.
 */
public record PaymentWebhookPayload(
        String paymentId,
        String orderId,
        String status,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) {
}
