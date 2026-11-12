package ir.bigz.messagequeue.producer.infrastructure.persistence;

import ir.bigz.messagequeue.producer.application.OrderRepository;
import ir.bigz.messagequeue.producer.domain.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaOrderRepository implements OrderRepository {

	private final SpringDataOrderRepository springDataOrderRepository;

	public JpaOrderRepository(SpringDataOrderRepository springDataOrderRepository) {
		this.springDataOrderRepository = springDataOrderRepository;
	}

	@Override
	public void save(Order order) {
		springDataOrderRepository.save(new OrderEntity(order.correlationId(), order.occurredAt(), order.status()));
	}

	@Override
	public Optional<Order> findByCorrelationId(String correlationId) {
		return springDataOrderRepository.findById(correlationId)
				.map(entity -> new Order(entity.getCorrelationId(), entity.getOccurredAt(), entity.getStatus()));
	}

}
