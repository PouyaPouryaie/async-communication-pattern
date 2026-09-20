package ir.bigz.webhooks.paymentApi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A merchant (an online store) that registered to receive payment webhooks.
 * The merchant supplies its own webhook URL and the shared secret used to
 * HMAC-sign every webhook this application delivers back to it.
 */
@Entity
@Table(name = "merchant_registrations", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = "webhook_url"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRegistration {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "webhook_url", nullable = false)
    private String webhookUrl;

    @Column(nullable = false)
    private String secret;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
