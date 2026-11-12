package ir.bigz.messagequeue.consumer.infrastructure.messaging;

import com.rabbitmq.client.Channel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class PoisonOrderEventListener {

	private final Counter poisonMessages;

	public PoisonOrderEventListener(MeterRegistry meterRegistry) {
		this.poisonMessages = meterRegistry.counter("order.events.poison.messages");
	}

	@RabbitListener(queues = OrderEventsTopologyConfig.DEAD_LETTER_QUEUE,
			containerFactory = "deadLetterListenerContainerFactory")
	public void onPoisonMessage(Message message, Channel channel,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
		log.error("Poison order event dead-lettered: body={}", new String(message.getBody(), StandardCharsets.UTF_8));
		poisonMessages.increment();
		channel.basicAck(deliveryTag, false);
	}

}
