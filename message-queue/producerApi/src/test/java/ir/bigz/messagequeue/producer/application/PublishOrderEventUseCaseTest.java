package ir.bigz.messagequeue.producer.application;

import ir.bigz.messagequeue.producer.domain.OrderPublishException;
import ir.bigz.messagequeue.producer.domain.OrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Testcontainers
class PublishOrderEventUseCaseTest {

	private static final String QUEUE = "order.events.queue";

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");

	@Autowired
	private PublishOrderEventUseCase publishOrderEvent;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private AmqpAdmin amqpAdmin;

	@Autowired
	private JdbcClient jdbcClient;

	@MockitoSpyBean
	private OrderEventPublisher orderEventPublisher;

	@BeforeEach
	void resetState() {
		jdbcClient.sql("DELETE FROM orders").update();
		jdbcClient.sql("DELETE FROM order_sequence").update();
		amqpAdmin.purgeQueue(QUEUE);
	}

	@AfterEach
	void resetPublisher() {
		Mockito.reset(orderEventPublisher);
	}

	@Test
	void publishesOrderEventAndMarksOrderPublished() {
		String correlationId = publishOrderEvent.publish();

		assertThat(correlationId).isEqualTo("order-00001");
		assertThat(orderRepository.findByCorrelationId(correlationId))
				.hasValueSatisfying(order -> assertThat(order.status()).isEqualTo(OrderStatus.PUBLISHED));

		Message message = rabbitTemplate.receive(QUEUE, 5_000);
		assertThat(message).isNotNull();
		assertThat(new String(message.getBody(), StandardCharsets.UTF_8))
				.contains("\"correlationId\":\"order-00001\"");
	}

	@Test
	void leavesOrderPendingWhenPublishFails() {
		doThrow(new OrderPublishException("broker nack")).when(orderEventPublisher).publish(any());

		assertThatThrownBy(() -> publishOrderEvent.publish()).isInstanceOf(OrderPublishException.class);

		Long pendingOrders = jdbcClient.sql("SELECT COUNT(*) FROM orders WHERE status = 'PENDING'")
				.query(Long.class).single();
		assertThat(pendingOrders).isEqualTo(1L);
	}

}
