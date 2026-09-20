package ir.bigz.webhooks.paymentApi.webhook;

import ir.bigz.webhooks.paymentApi.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The JSON body delivered to a merchant's webhook URL once a payment reaches
 * a terminal state. This is the wire contract with the merchant: only the
 * JSON shape and the HMAC signature matter, not this Java type itself.
 */
public record WebhookPayload(
        UUID paymentId,
        String orderId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) {
}
