package ir.bigz.webhooks.paymentApi.payment;

import ir.bigz.webhooks.paymentApi.domain.Payment;
import ir.bigz.webhooks.paymentApi.domain.PaymentStatus;
import ir.bigz.webhooks.paymentApi.exception.PaymentNotCancelableException;
import ir.bigz.webhooks.paymentApi.exception.PaymentNotFoundException;
import ir.bigz.webhooks.paymentApi.repository.MerchantRegistrationRepository;
import ir.bigz.webhooks.paymentApi.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentSettlementServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MerchantRegistrationRepository merchantRegistrationRepository;

    @Test
    void cancelPaymentRejectsPendingPayment() {
        PaymentSettlementService service = new PaymentSettlementService(paymentRepository, merchantRegistrationRepository);
        UUID paymentId = UUID.randomUUID();
        Payment payment = payment(paymentId, "order-1", PaymentStatus.PENDING);
        when(paymentRepository.findLockedById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.cancelPayment(paymentId, "order-1"))
                .isInstanceOf(PaymentNotCancelableException.class);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"SUCCEEDED", "FAILED"})
    void cancelPaymentMarksSettledPaymentCanceled(PaymentStatus settledStatus) {
        PaymentSettlementService service = new PaymentSettlementService(paymentRepository, merchantRegistrationRepository);
        UUID paymentId = UUID.randomUUID();
        Payment payment = payment(paymentId, "order-1", settledStatus);
        when(paymentRepository.findLockedById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        Payment canceled = service.cancelPayment(paymentId, "order-1");

        assertThat(canceled.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(canceled.getUpdatedAt()).isNotNull();
    }

    @Test
    void cancelPaymentRejectsOrderIdMismatch() {
        PaymentSettlementService service = new PaymentSettlementService(paymentRepository, merchantRegistrationRepository);
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findLockedById(paymentId))
                .thenReturn(Optional.of(payment(paymentId, "order-1", PaymentStatus.PENDING)));

        assertThatThrownBy(() -> service.cancelPayment(paymentId, "different-order"))
                .isInstanceOf(PaymentNotFoundException.class);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void cancelPaymentReturnsAlreadyCanceledPaymentForRetry() {
        PaymentSettlementService service = new PaymentSettlementService(paymentRepository, merchantRegistrationRepository);
        UUID paymentId = UUID.randomUUID();
        Payment payment = payment(paymentId, "order-1", PaymentStatus.CANCELED);
        when(paymentRepository.findLockedById(paymentId)).thenReturn(Optional.of(payment));

        Payment canceled = service.cancelPayment(paymentId, "order-1");

        assertThat(canceled.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void settlementDoesNotOverwriteCanceledPayment() {
        PaymentSettlementService service = new PaymentSettlementService(paymentRepository, merchantRegistrationRepository);
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findLockedById(paymentId))
                .thenReturn(Optional.of(payment(paymentId, "order-1", PaymentStatus.CANCELED)));

        assertThat(service.applyOutcome(paymentId, PaymentStatus.SUCCEEDED)).isFalse();
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    private Payment payment(UUID paymentId, String orderId, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setOrderId(orderId);
        payment.setStatus(status);
        return payment;
    }
}