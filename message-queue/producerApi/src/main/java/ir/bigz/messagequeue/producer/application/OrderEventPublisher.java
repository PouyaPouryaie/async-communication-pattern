package ir.bigz.messagequeue.producer.application;

import ir.bigz.messagequeue.producer.domain.OrderEvent;

public interface OrderEventPublisher {

	void publish(OrderEvent event);

}
