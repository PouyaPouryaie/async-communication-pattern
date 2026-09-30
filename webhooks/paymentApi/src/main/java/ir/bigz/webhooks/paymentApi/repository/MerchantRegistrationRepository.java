package ir.bigz.webhooks.paymentApi.repository;

import ir.bigz.webhooks.paymentApi.domain.MerchantRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MerchantRegistrationRepository extends JpaRepository<MerchantRegistration, UUID> {

    Optional<MerchantRegistration> findByWebhookUrl(String webhookUrl);
}
