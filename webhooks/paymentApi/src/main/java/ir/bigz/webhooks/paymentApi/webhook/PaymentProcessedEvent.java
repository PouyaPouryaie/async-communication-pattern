package ir.bigz.webhooks.paymentApi.webhook;

import java.util.UUID;

/**
 * Published once a simulated payment reaches a terminal state (succeeded or
 * failed). {@link PaymentEventListener} reacts to this event by delivering a
 * signed webhook to the merchant.
 */
public record PaymentProcessedEvent(UUID paymentId) {
}
