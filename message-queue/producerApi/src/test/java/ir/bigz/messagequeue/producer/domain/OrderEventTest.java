package ir.bigz.messagequeue.producer.domain;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OrderEventTest {

	@Test
	void serializesCorrelationIdAsJsonProperty() {
		var converter = new JacksonJsonMessageConverter();
		var event = new OrderEvent("order-00001", Instant.parse("2026-11-11T00:00:00Z"));

		Message message = converter.toMessage(event, new MessageProperties());

		String json = new String(message.getBody(), StandardCharsets.UTF_8);
		assertThat(json).contains("\"correlationId\":\"order-00001\"");
	}

}
