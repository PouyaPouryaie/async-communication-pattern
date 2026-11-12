package ir.bigz.messagequeue.consumer.domain;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OrderEventTest {

	@Test
	void deserializesProducerJsonIntoOrderEvent() {
		String producerJson = "{\"correlationId\":\"order-00001\",\"occurredAt\":\"2026-11-11T00:00:00Z\"}";
		var properties = new MessageProperties();
		properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
		properties.setHeader("__TypeId__", "ir.bigz.messagequeue.producer.domain.OrderEvent");
		properties.setInferredArgumentType(OrderEvent.class);
		Message message = new Message(producerJson.getBytes(StandardCharsets.UTF_8), properties);

		var converter = new JacksonJsonMessageConverter();
		OrderEvent event = (OrderEvent) converter.fromMessage(message);

		assertThat(event.correlationId()).isEqualTo("order-00001");
		assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-11-11T00:00:00Z"));
	}

}
