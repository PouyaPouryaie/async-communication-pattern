package ir.bigz.webhooks.paymentApi.registry;

import ir.bigz.webhooks.paymentApi.domain.MerchantRegistration;
import ir.bigz.webhooks.paymentApi.repository.MerchantRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RegistrationService {

    private final MerchantRegistrationRepository merchantRegistrationRepository;

    public RegistrationService(MerchantRegistrationRepository merchantRegistrationRepository) {
        this.merchantRegistrationRepository = merchantRegistrationRepository;
    }

    /**
     * Registers a merchant's webhook URL and secret, or updates the secret of
     * an already-registered URL. This makes registration safe to repeat, which
     * matters because the merchant re-registers on every application startup.
     */
    @Transactional
    public RegisterMerchantResponse register(RegisterMerchantRequest request) {
        Instant now = Instant.now();
        MerchantRegistration registration = merchantRegistrationRepository.findByWebhookUrl(request.webhookUrl())
                .orElseGet(() -> {
                    MerchantRegistration created = new MerchantRegistration();
                    created.setWebhookUrl(request.webhookUrl());
                    created.setCreatedAt(now);
                    return created;
                });
        registration.setSecret(request.secret());
        registration.setUpdatedAt(now);

        MerchantRegistration saved = merchantRegistrationRepository.save(registration);
        return new RegisterMerchantResponse(saved.getId());
    }
}
