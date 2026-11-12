package ir.bigz.messagequeue.consumer.infrastructure.persistence;

import ir.bigz.messagequeue.consumer.application.ProcessedEventLedger;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

@Component
public class JdbcProcessedEventLedger implements ProcessedEventLedger {

	// One atomic statement: the row is inserted only when the correlationId is new.
	private static final String RECORD_SQL = """
			INSERT INTO processed_order_events (correlation_id, processed_at)
			VALUES (:correlationId, :processedAt)
			ON CONFLICT (correlation_id) DO NOTHING
			""";

	private final JdbcClient jdbcClient;

	public JdbcProcessedEventLedger(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public boolean recordIfAbsent(String correlationId) {
		int inserted = jdbcClient.sql(RECORD_SQL)
				.param("correlationId", correlationId)
				.param("processedAt", Timestamp.from(Instant.now()))
				.update();
		return inserted == 1;
	}

}
