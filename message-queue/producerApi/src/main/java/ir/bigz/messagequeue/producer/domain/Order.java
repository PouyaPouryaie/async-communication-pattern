package ir.bigz.messagequeue.producer.domain;

import java.time.Instant;

public record Order(String correlationId, Instant occurredAt, OrderStatus status) {
}
