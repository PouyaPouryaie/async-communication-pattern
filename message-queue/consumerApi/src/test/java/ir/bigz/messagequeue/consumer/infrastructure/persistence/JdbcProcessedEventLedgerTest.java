package ir.bigz.messagequeue.consumer.infrastructure.persistence;

import ir.bigz.messagequeue.consumer.application.ProcessedEventLedger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class JdbcProcessedEventLedgerTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");

	@Autowired
	private ProcessedEventLedger ledger;

	@Autowired
	private JdbcClient jdbcClient;

	@BeforeEach
	void resetLedger() {
		jdbcClient.sql("DELETE FROM processed_order_events").update();
	}

	@Test
	void recordsNewEventInLedger() {
		assertThat(ledger.recordIfAbsent("order-00001")).isTrue();

		Long rows = jdbcClient.sql("SELECT COUNT(*) FROM processed_order_events WHERE correlation_id = 'order-00001'")
				.query(Long.class).single();
		assertThat(rows).isEqualTo(1L);
	}

}
