package ir.bigz.messagequeue.producer.infrastructure.persistence;

import ir.bigz.messagequeue.producer.application.OrderNumberSequence;
import ir.bigz.messagequeue.producer.domain.OrderNumberExhaustedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class OrderNumberSequenceTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Autowired
	private OrderNumberSequence orderNumberSequence;

	@Autowired
	private JdbcClient jdbcClient;

	@BeforeEach
	void resetSequence() {
		jdbcClient.sql("DELETE FROM order_sequence").update();
	}

	@Test
	void issuesSequentialOrderNumbers() {
		assertThat(orderNumberSequence.next()).isEqualTo("order-00001");
		assertThat(orderNumberSequence.next()).isEqualTo("order-00002");
	}

	@Test
	void failsWhenSequenceIsExhausted() {
		jdbcClient.sql("INSERT INTO order_sequence (id, last_value) VALUES (1, 99999)").update();

		assertThatThrownBy(() -> orderNumberSequence.next())
				.isInstanceOf(OrderNumberExhaustedException.class);
	}

}
