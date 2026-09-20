package ir.bigz.webhooks.orderApi.repository;

import ir.bigz.webhooks.orderApi.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
