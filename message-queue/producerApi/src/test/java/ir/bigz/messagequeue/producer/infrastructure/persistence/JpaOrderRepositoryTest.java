package ir.bigz.messagequeue.producer.infrastructure.persistence;

import ir.bigz.messagequeue.producer.application.OrderRepository;
import ir.bigz.messagequeue.producer.domain.Order;
import ir.bigz.messagequeue.producer.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class JpaOrderRepositoryTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Autowired
	private OrderRepository orderRepository;

	@Test
	void savesPendingOrderAndReadsItBack() {
		orderRepository.save(new Order("order-00001", Instant.parse("2026-11-11T00:00:00Z"), OrderStatus.PENDING));

		assertThat(orderRepository.findByCorrelationId("order-00001")).hasValueSatisfying(found -> {
			assertThat(found.correlationId()).isEqualTo("order-00001");
			assertThat(found.status()).isEqualTo(OrderStatus.PENDING);
		});
	}

}
