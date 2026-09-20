package ir.bigz.webhooks.paymentApi.payment;

import ir.bigz.webhooks.paymentApi.domain.Payment;
import ir.bigz.webhooks.paymentApi.domain.PaymentStatus;
import ir.bigz.webhooks.paymentApi.exception.MerchantNotFoundException;
import ir.bigz.webhooks.paymentApi.repository.MerchantRegistrationRepository;
import ir.bigz.webhooks.paymentApi.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MerchantRegistrationRepository merchantRegistrationRepository;

    @Mock
    private PaymentProcessingSimulator paymentProcessingSimulator;

    private PaymentService paymentService;

    @Test
    void initiatePaymentSavesPendingPaymentAndTriggersSimulation() {
        paymentService = new PaymentService(paymentRepository, merchantRegistrationRepository, paymentProcessingSimulator);
        UUID merchantId = UUID.randomUUID();
        when(merchantRegistrationRepository.existsById(merchantId)).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(UUID.randomUUID());
            return payment;
        });

        PaymentInitiatedResponse response = paymentService.initiatePayment(
                new InitiatePaymentRequest(merchantId, "order-1", new BigDecimal("49.99"), "USD"));

        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.paymentId()).isNotNull();
        verify(paymentProcessingSimulator).simulate(response.paymentId());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo("order-1");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("49.99");
    }

    @Test
    void initiatePaymentRejectsUnknownMerchant() {
        paymentService = new PaymentService(paymentRepository, merchantRegistrationRepository, paymentProcessingSimulator);
        UUID merchantId = UUID.randomUUID();
        when(merchantRegistrationRepository.existsById(merchantId)).thenReturn(false);

        assertThatThrownBy(() -> paymentService.initiatePayment(
                new InitiatePaymentRequest(merchantId, "order-1", new BigDecimal("49.99"), "USD")))
                .isInstanceOf(MerchantNotFoundException.class);
    }
}
