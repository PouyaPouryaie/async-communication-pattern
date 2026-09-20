package ir.bigz.webhooks.paymentApi.payment;

import ir.bigz.webhooks.paymentApi.domain.Payment;
import ir.bigz.webhooks.paymentApi.domain.PaymentStatus;
import ir.bigz.webhooks.paymentApi.exception.MerchantNotFoundException;
import ir.bigz.webhooks.paymentApi.repository.MerchantRegistrationRepository;
import ir.bigz.webhooks.paymentApi.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MerchantRegistrationRepository merchantRegistrationRepository;
    private final PaymentProcessingSimulator paymentProcessingSimulator;

    public PaymentService(PaymentRepository paymentRepository,
                           MerchantRegistrationRepository merchantRegistrationRepository,
                           PaymentProcessingSimulator paymentProcessingSimulator) {
        this.paymentRepository = paymentRepository;
        this.merchantRegistrationRepository = merchantRegistrationRepository;
        this.paymentProcessingSimulator = paymentProcessingSimulator;
    }

    /**
     * Records a pending payment and kicks off asynchronous processing.
     * Returns immediately so the calling merchant's request does not block
     * on the (simulated) payment provider — the merchant is notified of the
     * outcome later via a webhook.
     *
     * <p>The pending payment is persisted and committed first; only then is
     * the async simulator triggered. Otherwise, the background thread could
     * start looking up the payment before this method's transaction commits.
     */
    public PaymentInitiatedResponse initiatePayment(InitiatePaymentRequest request) {
        Payment saved = createPendingPayment(request);
        paymentProcessingSimulator.simulate(saved.getId());
        return new PaymentInitiatedResponse(saved.getId(), saved.getStatus());
    }

    @Transactional
    Payment createPendingPayment(InitiatePaymentRequest request) {
        if (!merchantRegistrationRepository.existsById(request.merchantId())) {
            throw new MerchantNotFoundException(request.merchantId().toString());
        }

        Instant now = Instant.now();
        Payment payment = new Payment();
        payment.setMerchantId(request.merchantId());
        payment.setOrderId(request.orderId());
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        return paymentRepository.save(payment);
    }
}
