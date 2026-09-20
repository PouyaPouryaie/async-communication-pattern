package ir.bigz.webhooks.paymentApi.registry;

import jakarta.validation.constraints.NotBlank;

/**
 * Sent by a merchant (the online store) to register its webhook URL and the
 * shared secret used to verify signed webhook deliveries.
 */
public record RegisterMerchantRequest(

        @NotBlank(message = "webhookUrl is required")
        String webhookUrl,

        @NotBlank(message = "secret is required")
        String secret
) {
}
