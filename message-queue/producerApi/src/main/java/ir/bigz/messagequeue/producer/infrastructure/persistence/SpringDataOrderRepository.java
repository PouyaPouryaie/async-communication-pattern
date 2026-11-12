package ir.bigz.messagequeue.producer.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOrderRepository extends JpaRepository<OrderEntity, String> {
}
