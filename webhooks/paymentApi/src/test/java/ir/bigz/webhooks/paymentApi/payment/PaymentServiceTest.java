package ir.bigz.webhooks.paymentApi.payment;

import ir.bigz.webhooks.paymentApi.domain.Payment;
import ir.bigz.webhooks.paymentApi.domain.PaymentStatus;
import ir.bigz.webhooks.paymentApi.exception.MerchantNotFoundException;
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
    private PaymentSettlementService paymentSettlementService;

    @Mock
    private PaymentProcessingSimulator paymentProcessingSimulator;

    private PaymentService paymentService;

    @Test
    void initiatePaymentSettlesPendingPaymentAndTriggersSimulation() {
        paymentService = new PaymentService(paymentSettlementService, paymentProcessingSimulator);
        UUID merchantId = UUID.randomUUID();
        Payment savedPayment = new Payment();
        savedPayment.setId(UUID.randomUUID());
        savedPayment.setStatus(PaymentStatus.PENDING);
        when(paymentSettlementService.createPendingPayment(any(InitiatePaymentRequest.class))).thenReturn(savedPayment);

        InitiatePaymentRequest request =
                new InitiatePaymentRequest(merchantId, "order-1", new BigDecimal("49.99"), "USD");
        PaymentInitiatedResponse response = paymentService.initiatePayment(request);

        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.paymentId()).isEqualTo(savedPayment.getId());
        verify(paymentProcessingSimulator).simulate(response.paymentId());

        ArgumentCaptor<InitiatePaymentRequest> captor = ArgumentCaptor.forClass(InitiatePaymentRequest.class);
        verify(paymentSettlementService).createPendingPayment(captor.capture());
        assertThat(captor.getValue()).isEqualTo(request);
    }

    @Test
    void initiatePaymentPropagatesUnknownMerchant() {
        paymentService = new PaymentService(paymentSettlementService, paymentProcessingSimulator);
        UUID merchantId = UUID.randomUUID();
        when(paymentSettlementService.createPendingPayment(any(InitiatePaymentRequest.class)))
                .thenThrow(new MerchantNotFoundException(merchantId.toString()));

        assertThatThrownBy(() -> paymentService.initiatePayment(
                new InitiatePaymentRequest(merchantId, "order-1", new BigDecimal("49.99"), "USD")))
                .isInstanceOf(MerchantNotFoundException.class);
    }
}
