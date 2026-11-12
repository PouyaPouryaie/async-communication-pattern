package ir.bigz.messagequeue.consumer.infrastructure.messaging;

import ir.bigz.messagequeue.consumer.application.ProcessOrderEventUseCase;
import ir.bigz.messagequeue.consumer.domain.OrderEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.time.Instant;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
class OrderEventConsumptionTest {

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@MockitoSpyBean
	private ProcessOrderEventUseCase processOrderEvent;

	@AfterEach
	void resetProcessor() {
		Mockito.reset(processOrderEvent);
	}

	@Test
	void consumesPublishedOrderEvent() {
		var event = new OrderEvent("order-00001", Instant.parse("2026-11-11T00:00:00Z"));

		rabbitTemplate.convertAndSend(OrderEventsTopologyConfig.EXCHANGE, OrderEventsTopologyConfig.ROUTING_KEY, event);

		verify(processOrderEvent, timeout(5_000)).process(event);
	}

}
