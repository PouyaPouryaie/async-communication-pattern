package ir.bigz.webhooks.orderApi.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("No order found with id " + orderId);
    }

    public OrderNotFoundException(UUID orderId) {
        this(orderId.toString());
    }
}
