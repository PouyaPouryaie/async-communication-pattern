package ir.bigz.messagequeue.consumer.infrastructure.messaging;

import ir.bigz.messagequeue.consumer.application.ProcessOrderEventUseCase;
import ir.bigz.messagequeue.consumer.domain.OrderEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class OrderEventDuplicateTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private AmqpAdmin amqpAdmin;

	@Autowired
	private JdbcClient jdbcClient;

	@MockitoSpyBean
	private ProcessOrderEventUseCase processOrderEvent;

	@BeforeEach
	void resetState() {
		jdbcClient.sql("DELETE FROM processed_order_events").update();
		amqpAdmin.purgeQueue(OrderEventsTopologyConfig.QUEUE);
	}

	@AfterEach
	void resetProcessor() {
		Mockito.reset(processOrderEvent);
	}

	@Test
	void processesRedeliveredEventOnlyOnce(CapturedOutput output) throws InterruptedException {
		var event = new OrderEvent("order-00001", Instant.parse("2026-11-11T00:00:00Z"));

		rabbitTemplate.convertAndSend(OrderEventsTopologyConfig.EXCHANGE, OrderEventsTopologyConfig.ROUTING_KEY, event);
		rabbitTemplate.convertAndSend(OrderEventsTopologyConfig.EXCHANGE, OrderEventsTopologyConfig.ROUTING_KEY, event);

		verify(processOrderEvent, timeout(5_000).times(2)).process(event);
		awaitLogLines(output, "Duplicate order event skipped", 1);

		assertThat(countOccurrences(output.getAll(), "Order event received: correlationId=order-00001")).isEqualTo(1);
		assertThat(countOccurrences(output.getAll(), "Duplicate order event skipped: correlationId=order-00001")).isEqualTo(1);
	}

	private static void awaitLogLines(CapturedOutput output, String text, int expected) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 5_000;
		while (countOccurrences(output.getAll(), text) < expected && System.currentTimeMillis() < deadline) {
			Thread.sleep(50);
		}
	}

	private static long countOccurrences(String text, String needle) {
		return text.split(java.util.regex.Pattern.quote(needle), -1).length - 1;
	}

}
