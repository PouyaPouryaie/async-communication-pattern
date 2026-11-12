package ir.bigz.messagequeue.consumer.infrastructure.messaging;

import com.rabbitmq.client.Channel;
import ir.bigz.messagequeue.consumer.application.ProcessOrderEventUseCase;
import ir.bigz.messagequeue.consumer.domain.OrderEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OrderEventListener {

	private final ProcessOrderEventUseCase processOrderEvent;

	public OrderEventListener(ProcessOrderEventUseCase processOrderEvent) {
		this.processOrderEvent = processOrderEvent;
	}

	@RabbitListener(queues = OrderEventsTopologyConfig.QUEUE)
	public void onOrderEvent(OrderEvent event, Channel channel,
			@Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
		processOrderEvent.process(event);
		channel.basicAck(deliveryTag, false);
	}

}
