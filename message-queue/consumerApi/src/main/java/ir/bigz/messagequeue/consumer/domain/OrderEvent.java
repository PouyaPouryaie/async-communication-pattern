package ir.bigz.messagequeue.consumer.domain;

import java.time.Instant;

public record OrderEvent(String correlationId, Instant occurredAt) {
}
