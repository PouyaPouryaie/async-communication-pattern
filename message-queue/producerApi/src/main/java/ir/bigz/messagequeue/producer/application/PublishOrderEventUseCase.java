package ir.bigz.messagequeue.producer.application;

import ir.bigz.messagequeue.producer.domain.Order;
import ir.bigz.messagequeue.producer.domain.OrderEvent;
import ir.bigz.messagequeue.producer.domain.OrderStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PublishOrderEventUseCase {

	private final OrderNumberSequence orderNumberSequence;
	private final OrderRepository orderRepository;
	private final OrderEventPublisher orderEventPublisher;

	public PublishOrderEventUseCase(OrderNumberSequence orderNumberSequence,
			OrderRepository orderRepository,
			OrderEventPublisher orderEventPublisher) {
		this.orderNumberSequence = orderNumberSequence;
		this.orderRepository = orderRepository;
		this.orderEventPublisher = orderEventPublisher;
	}

	public String publish() {
		String correlationId = orderNumberSequence.next();
		Instant occurredAt = Instant.now();

		orderRepository.save(new Order(correlationId, occurredAt, OrderStatus.PENDING));
		orderEventPublisher.publish(new OrderEvent(correlationId, occurredAt));
		orderRepository.save(new Order(correlationId, occurredAt, OrderStatus.PUBLISHED));

		return correlationId;
	}

}
