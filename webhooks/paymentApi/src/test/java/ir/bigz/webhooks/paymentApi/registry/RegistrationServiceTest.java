package ir.bigz.webhooks.paymentApi.registry;

import ir.bigz.webhooks.paymentApi.domain.MerchantRegistration;
import ir.bigz.webhooks.paymentApi.repository.MerchantRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private MerchantRegistrationRepository merchantRegistrationRepository;

    private RegistrationService registrationService;

    @Test
    void registerCreatesNewMerchantWhenWebhookUrlIsUnknown() {
        registrationService = new RegistrationService(merchantRegistrationRepository);
        when(merchantRegistrationRepository.findByWebhookUrl("https://store.example/webhooks/payments"))
                .thenReturn(Optional.empty());
        when(merchantRegistrationRepository.save(any(MerchantRegistration.class)))
                .thenAnswer(invocation -> {
                    MerchantRegistration argument = invocation.getArgument(0);
                    argument.setId(UUID.randomUUID());
                    return argument;
                });

        RegisterMerchantResponse response = registrationService.register(
                new RegisterMerchantRequest("https://store.example/webhooks/payments", "shared-secret"));

        assertThat(response.merchantId()).isNotNull();

        ArgumentCaptor<MerchantRegistration> captor = ArgumentCaptor.forClass(MerchantRegistration.class);
        verify(merchantRegistrationRepository).save(captor.capture());
        assertThat(captor.getValue().getWebhookUrl()).isEqualTo("https://store.example/webhooks/payments");
        assertThat(captor.getValue().getSecret()).isEqualTo("shared-secret");
    }

    @Test
    void registerUpdatesSecretAndReusesMerchantIdWhenWebhookUrlAlreadyRegistered() {
        registrationService = new RegistrationService(merchantRegistrationRepository);
        UUID existingId = UUID.randomUUID();
        MerchantRegistration existing = new MerchantRegistration();
        existing.setId(existingId);
        existing.setWebhookUrl("https://store.example/webhooks/payments");
        existing.setSecret("old-secret");
        existing.setCreatedAt(Instant.now().minusSeconds(60));
        when(merchantRegistrationRepository.findByWebhookUrl("https://store.example/webhooks/payments"))
                .thenReturn(Optional.of(existing));
        when(merchantRegistrationRepository.save(any(MerchantRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterMerchantResponse response = registrationService.register(
                new RegisterMerchantRequest("https://store.example/webhooks/payments", "new-secret"));

        assertThat(response.merchantId()).isEqualTo(existingId);
        assertThat(existing.getSecret()).isEqualTo("new-secret");
    }
}
