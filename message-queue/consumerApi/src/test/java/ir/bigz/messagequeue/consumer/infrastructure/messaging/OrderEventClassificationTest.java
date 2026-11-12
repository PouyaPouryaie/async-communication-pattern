package ir.bigz.messagequeue.consumer.infrastructure.messaging;

import ir.bigz.messagequeue.consumer.application.ProcessOrderEventUseCase;
import ir.bigz.messagequeue.consumer.domain.OrderEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
class OrderEventClassificationTest {

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private AmqpAdmin amqpAdmin;

	@MockitoSpyBean
	private ProcessOrderEventUseCase processOrderEvent;

	@BeforeEach
	void resetQueues() {
		amqpAdmin.purgeQueue(OrderEventsTopologyConfig.QUEUE);
		amqpAdmin.purgeQueue(OrderEventsTopologyConfig.DEAD_LETTER_QUEUE);
	}

	@AfterEach
	void resetProcessor() {
		Mockito.reset(processOrderEvent);
	}

	@Test
	void deadLettersMalformedPayloadWithoutProcessing() {
		var properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		rabbitTemplate.send(OrderEventsTopologyConfig.EXCHANGE, OrderEventsTopologyConfig.ROUTING_KEY,
				new Message("{not-json".getBytes(StandardCharsets.UTF_8), properties));

		Message deadLettered = rabbitTemplate.receive(OrderEventsTopologyConfig.DEAD_LETTER_QUEUE, 5_000);

		assertThat(deadLettered).isNotNull();
		assertThat(new String(deadLettered.getBody(), StandardCharsets.UTF_8)).isEqualTo("{not-json");
		verify(processOrderEvent, never()).process(any());
	}

	@Test
	void deadLettersBlankCorrelationIdWithoutRetry() {
		rabbitTemplate.convertAndSend(OrderEventsTopologyConfig.EXCHANGE, OrderEventsTopologyConfig.ROUTING_KEY,
				new OrderEvent(" ", Instant.parse("2026-11-11T00:00:00Z")));

		Message deadLettered = rabbitTemplate.receive(OrderEventsTopologyConfig.DEAD_LETTER_QUEUE, 5_000);

		assertThat(deadLettered).isNotNull();
		verify(processOrderEvent, times(1)).process(any());
	}

}
