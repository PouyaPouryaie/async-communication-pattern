package ir.bigz.webhooks.orderApi.domain;

public enum OrderStatus {
    PENDING,
    PAYMENT_PROCESSING,
    PAID,
    PAYMENT_FAILED,
    CANCELED
}
