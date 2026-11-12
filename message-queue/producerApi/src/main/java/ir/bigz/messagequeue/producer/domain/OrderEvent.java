package ir.bigz.messagequeue.producer.domain;

import java.time.Instant;

public record OrderEvent(String correlationId, Instant occurredAt) {
}
