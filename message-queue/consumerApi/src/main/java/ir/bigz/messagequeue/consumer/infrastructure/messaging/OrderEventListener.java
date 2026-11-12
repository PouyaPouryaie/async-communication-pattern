package ir.bigz.messagequeue.consumer.infrastructure.messaging;

import com.rabbitmq.client.Channel;
import ir.bigz.messagequeue.consumer.application.ProcessOrderEventUseCase;
import ir.bigz.messagequeue.consumer.domain.InvalidOrderEventException;
import ir.bigz.messagequeue.consumer.domain.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class OrderEventListener {

	private final ProcessOrderEventUseCase processOrderEvent;

	public OrderEventListener(ProcessOrderEventUseCase processOrderEvent) {
		this.processOrderEvent = processOrderEvent;
	}

	@RabbitListener(queues = OrderEventsTopologyConfig.QUEUE)
	public void onOrderEvent(OrderEvent event, Channel channel,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
		try {
			processOrderEvent.process(event);
			channel.basicAck(deliveryTag, false);
		} catch (InvalidOrderEventException e) {
			log.error("Rejecting unrecoverable order event to dead-letter queue: {}", e.getMessage());
			channel.basicNack(deliveryTag, false, false);
		}
	}

}
