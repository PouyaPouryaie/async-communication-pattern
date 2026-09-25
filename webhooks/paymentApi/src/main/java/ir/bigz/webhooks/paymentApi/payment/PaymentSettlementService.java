package ir.bigz.webhooks.paymentApi.payment;

import ir.bigz.webhooks.paymentApi.domain.Payment;
import ir.bigz.webhooks.paymentApi.domain.PaymentStatus;
import ir.bigz.webhooks.paymentApi.exception.MerchantNotFoundException;
import ir.bigz.webhooks.paymentApi.repository.MerchantRegistrationRepository;
import ir.bigz.webhooks.paymentApi.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentSettlementService {

    private static final Logger log = LoggerFactory.getLogger(PaymentSettlementService.class);
    private final PaymentRepository paymentRepository;
    private final MerchantRegistrationRepository merchantRegistrationRepository;

    public PaymentSettlementService(PaymentRepository paymentRepository, MerchantRegistrationRepository merchantRegistrationRepository) {
        this.paymentRepository = paymentRepository;
        this.merchantRegistrationRepository = merchantRegistrationRepository;
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

    @Transactional
    void applyOutcome(UUID paymentId, PaymentStatus outcome) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("Payment " + paymentId + " disappeared during processing"));
        payment.setStatus(outcome);
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);
        log.info("Payment {} settled with outcome {}", paymentId, outcome);
    }
}
