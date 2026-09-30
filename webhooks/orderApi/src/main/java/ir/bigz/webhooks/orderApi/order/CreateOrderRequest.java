package ir.bigz.webhooks.orderApi.order;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Submitted by a customer to place an order for a product.
 */
public record CreateOrderRequest(

        @NotBlank(message = "customerEmail is required")
        @Email(message = "customerEmail must be a valid email address")
        String customerEmail,

        @NotNull(message = "productId is required")
        Long productId,

        @Min(value = 1, message = "quantity must be at least 1")
        int quantity
) {
}
