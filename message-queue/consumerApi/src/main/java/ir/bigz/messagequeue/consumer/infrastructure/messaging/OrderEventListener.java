package ir.bigz.messagequeue.consumer.infrastructure.messaging;

import com.rabbitmq.client.Channel;
import ir.bigz.messagequeue.consumer.application.ProcessOrderEventUseCase;
import ir.bigz.messagequeue.consumer.domain.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class OrderEventListener {

	private final ProcessOrderEventUseCase processOrderEvent;
	private final RetryTemplate retryTemplate;

	public OrderEventListener(ProcessOrderEventUseCase processOrderEvent, RetryTemplate retryTemplate) {
		this.processOrderEvent = processOrderEvent;
		this.retryTemplate = retryTemplate;
	}

	@RabbitListener(queues = OrderEventsTopologyConfig.QUEUE)
	public void onOrderEvent(OrderEvent event, Channel channel,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
		try {
			retryTemplate.invoke(() -> processOrderEvent.process(event));
			channel.basicAck(deliveryTag, false);
		} catch (RuntimeException e) {
			log.error("Rejecting order event to dead-letter queue after retries: {}", e.getMessage());
			channel.basicNack(deliveryTag, false, false);
		}
	}

}
