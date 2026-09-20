package ir.bigz.webhooks.paymentApi.payment;

import ir.bigz.webhooks.paymentApi.domain.PaymentStatus;

import java.util.UUID;

public record PaymentInitiatedResponse(UUID paymentId, PaymentStatus status) {
}
