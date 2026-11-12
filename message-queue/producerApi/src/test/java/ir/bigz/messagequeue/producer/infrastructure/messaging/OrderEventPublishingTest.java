package ir.bigz.messagequeue.producer.infrastructure.messaging;

import ir.bigz.messagequeue.producer.domain.OrderEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class OrderEventPublishingTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Test
	void publishesPersistentJsonOrderEventAndGetsConfirm() throws Exception {
		var event = new OrderEvent("order-00001", Instant.parse("2026-11-11T00:00:00Z"));
		var correlationData = new CorrelationData("order-00001");

		rabbitTemplate.convertAndSend(OrderEventsTopologyConfig.EXCHANGE, OrderEventsTopologyConfig.ROUTING_KEY,
				event, correlationData);

		assertThat(correlationData.getFuture().get(10, TimeUnit.SECONDS).isAck()).isTrue();

		Message message = rabbitTemplate.receive(OrderEventsTopologyConfig.QUEUE, 5_000);
		assertThat(message).isNotNull();
		assertThat(message.getMessageProperties().getReceivedDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
		assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
		assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).contains("\"correlationId\":\"order-00001\"");
	}

}
