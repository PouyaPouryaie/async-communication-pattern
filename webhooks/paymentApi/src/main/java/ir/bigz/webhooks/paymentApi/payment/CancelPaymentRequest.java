package ir.bigz.webhooks.paymentApi.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CancelPaymentRequest(
        @NotBlank(message = "orderId is required")
        String orderId,

        @NotNull(message = "paymentId is required")
        UUID paymentId
) {
}