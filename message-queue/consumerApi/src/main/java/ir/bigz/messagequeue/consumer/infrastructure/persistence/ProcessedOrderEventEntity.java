package ir.bigz.messagequeue.consumer.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "processed_order_events")
public class ProcessedOrderEventEntity {

	@Id
	@Column(name = "correlation_id", nullable = false, updatable = false)
	private String correlationId;

	@Column(name = "processed_at", nullable = false)
	private Instant processedAt;

	protected ProcessedOrderEventEntity() {
	}

}
