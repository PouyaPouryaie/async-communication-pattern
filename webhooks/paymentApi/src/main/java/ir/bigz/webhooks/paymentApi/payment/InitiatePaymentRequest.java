package ir.bigz.webhooks.paymentApi.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Sent by a merchant to start a simulated payment for one of its orders.
 */
public record InitiatePaymentRequest(

        @NotNull(message = "merchantId is required")
        UUID merchantId,

        @NotBlank(message = "orderId is required")
        String orderId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        String currency
) {
}
