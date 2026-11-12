package ir.bigz.messagequeue.consumer.infrastructure.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class PoisonOrderEventListenerTest {

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
	private MeterRegistry meterRegistry;

	@BeforeEach
	void resetQueues() {
		amqpAdmin.purgeQueue(OrderEventsTopologyConfig.QUEUE);
		amqpAdmin.purgeQueue(OrderEventsTopologyConfig.DEAD_LETTER_QUEUE);
	}

	@Test
	void countsAndLogsDeadLetteredPoisonMessage(CapturedOutput output) throws InterruptedException {
		double baseline = PoisonMessageCounter.count(meterRegistry);
		var properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		rabbitTemplate.send(OrderEventsTopologyConfig.EXCHANGE, OrderEventsTopologyConfig.ROUTING_KEY,
				new Message("{poison".getBytes(StandardCharsets.UTF_8), properties));

		PoisonMessageCounter.awaitIncrement(meterRegistry, baseline);

		assertThat(PoisonMessageCounter.count(meterRegistry)).isEqualTo(baseline + 1);
		assertThat(output.getAll()).contains("Poison order event dead-lettered");
	}

}
