package ir.bigz.webhooks.orderApi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Idempotency ledger: one row per successfully processed payment webhook.
 * The unique constraint on {@code payment_id} is the database-layer guard
 * against processing the same webhook delivery twice (e.g. after a retry
 * from the payment application); {@code PaymentWebhookService} also checks
 * for an existing row first as a fast-path, service-layer guard.
 */
@Entity
@Table(name = "processed_webhook_events", uniqueConstraints = @UniqueConstraint(columnNames = "payment_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
