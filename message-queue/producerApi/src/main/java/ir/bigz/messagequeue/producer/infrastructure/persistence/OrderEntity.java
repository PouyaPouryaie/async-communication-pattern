package ir.bigz.messagequeue.producer.infrastructure.persistence;

import ir.bigz.messagequeue.producer.domain.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class OrderEntity {

	@Id
	@Column(name = "correlation_id", nullable = false, updatable = false)
	private String correlationId;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrderStatus status;

	public OrderEntity(String correlationId, Instant occurredAt, OrderStatus status) {
		this.correlationId = correlationId;
		this.occurredAt = occurredAt;
		this.status = status;
	}

}
