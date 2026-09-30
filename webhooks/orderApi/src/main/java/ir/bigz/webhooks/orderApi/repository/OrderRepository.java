package ir.bigz.webhooks.orderApi.repository;

import ir.bigz.webhooks.orderApi.domain.Order;
import ir.bigz.webhooks.orderApi.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByStatusAndUpdatedAtBefore(OrderStatus status, Instant cutoff);

    @Modifying
    @Transactional
	    @Query("update Order existingOrder set existingOrder.status = :canceledStatus, existingOrder.updatedAt = :updatedAt " +
		    "where existingOrder.id = :orderId and existingOrder.paymentId = :paymentId " +
		    "and existingOrder.status = :processingStatus")
    int markCanceledIfProcessing(@Param("orderId") UUID orderId,
				 @Param("paymentId") String paymentId,
				 @Param("updatedAt") Instant updatedAt,
				 @Param("processingStatus") OrderStatus processingStatus,
				 @Param("canceledStatus") OrderStatus canceledStatus);
}
