package ir.bigz.messagequeue.producer.application;

import ir.bigz.messagequeue.producer.domain.Order;

import java.util.Optional;

public interface OrderRepository {

	void save(Order order);

	Optional<Order> findByCorrelationId(String correlationId);

}
