package ir.bigz.messagequeue.producer.infrastructure.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class OrderEventsTopologyTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");

	@Autowired
	private Queue orderEventsQueue;

	@Autowired
	private Binding orderEventsBinding;

	@Autowired
	private AmqpAdmin amqpAdmin;

	@Test
	void declaresDurableQuorumQueueWithDeadLetterExchangeAndTtl() {
		assertThat(orderEventsQueue.getName()).isEqualTo("order.events.queue");
		assertThat(orderEventsQueue.isDurable()).isTrue();
		assertThat(orderEventsQueue.getArguments())
				.containsEntry("x-queue-type", "quorum")
				.containsEntry("x-dead-letter-exchange", "order.events.dlx")
				.containsEntry("x-message-ttl", 60000);
	}

	@Test
	void bindsOrderCreatedRoutingKeyToOrderEventsExchange() {
		assertThat(orderEventsBinding.getExchange()).isEqualTo("order.events");
		assertThat(orderEventsBinding.getRoutingKey()).isEqualTo("order.created");
		assertThat(orderEventsBinding.getDestination()).isEqualTo("order.events.queue");
	}

	@Test
	void declaresQueueOnBroker() {
		assertThat(amqpAdmin.getQueueProperties("order.events.queue")).isNotNull();
	}

}
