package ir.bigz.messagequeue.producer.infrastructure.messaging;

import ir.bigz.messagequeue.producer.application.OrderEventPublisher;
import ir.bigz.messagequeue.producer.domain.OrderEvent;
import ir.bigz.messagequeue.producer.domain.OrderPublishException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class RabbitOrderEventPublisher implements OrderEventPublisher {

	private static final long CONFIRM_TIMEOUT_SECONDS = 5;

	private final RabbitTemplate rabbitTemplate;

	public RabbitOrderEventPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	@Override
	public void publish(OrderEvent event) {
		var correlationData = new CorrelationData(event.correlationId());
		rabbitTemplate.convertAndSend(OrderEventsTopologyConfig.EXCHANGE, OrderEventsTopologyConfig.ROUTING_KEY,
				event, correlationData);
		try {
			CorrelationData.Confirm confirm = correlationData.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			if (!confirm.isAck()) {
				throw new OrderPublishException("Broker nacked " + event.correlationId() + ": " + confirm.getReason());
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new OrderPublishException("Interrupted while awaiting confirm for " + event.correlationId(), e);
		} catch (ExecutionException | TimeoutException e) {
			throw new OrderPublishException("No broker confirm for " + event.correlationId(), e);
		}
	}

}
