package ir.bigz.webhooks.orderApi.repository;

import ir.bigz.webhooks.orderApi.domain.ProcessedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, Long> {

    boolean existsByPaymentId(String paymentId);

    /**
     * Inserts the idempotency marker only if it does not already exist,
     * relying on the unique constraint on {@code payment_id}. Using
     * {@code ON CONFLICT DO NOTHING} instead of catching a constraint
     * violation avoids aborting the enclosing Postgres transaction, so the
     * rest of the webhook processing can safely continue in the same
     * transaction when this returns 1 (inserted).
     *
     * @return 1 if a new row was inserted, 0 if a row for this payment id already existed
     */
    @Modifying
    @Query(value = "INSERT INTO processed_webhook_events (payment_id, processed_at) "
            + "VALUES (:paymentId, :processedAt) ON CONFLICT (payment_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("paymentId") String paymentId, @Param("processedAt") Instant processedAt);
}
