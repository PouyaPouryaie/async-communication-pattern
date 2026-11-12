package ir.bigz.messagequeue.producer.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_sequence")
public class OrderSequenceEntity {

	@Id
	private Long id;

	@Column(name = "last_value", nullable = false)
	private long lastValue;

	protected OrderSequenceEntity() {
	}

}
