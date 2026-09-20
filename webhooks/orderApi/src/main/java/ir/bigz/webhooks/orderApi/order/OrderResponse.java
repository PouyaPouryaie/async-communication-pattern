package ir.bigz.webhooks.orderApi.order;

import ir.bigz.webhooks.orderApi.domain.Order;
import ir.bigz.webhooks.orderApi.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(UUID orderId, OrderStatus status, BigDecimal totalAmount, String currency) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getStatus(), order.getTotalAmount(), order.getCurrency());
    }
}
