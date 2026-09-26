package ir.bigz.webhooks.paymentApi.payment;

import ir.bigz.webhooks.paymentApi.domain.Payment;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentSettlementService paymentSettlementService;
    private final PaymentProcessingSimulator paymentProcessingSimulator;

    public PaymentService(PaymentSettlementService paymentSettlementService,
                           PaymentProcessingSimulator paymentProcessingSimulator) {
        this.paymentSettlementService = paymentSettlementService;
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
        Payment saved = paymentSettlementService.createPendingPayment(request);
        paymentProcessingSimulator.simulate(saved.getId());
        return new PaymentInitiatedResponse(saved.getId(), saved.getStatus());
    }

    public CancelPaymentResponse cancelPayment(CancelPaymentRequest request) {
        Payment canceled = paymentSettlementService.cancelPayment(request.paymentId(), request.orderId());
        return new CancelPaymentResponse(canceled.getId(), canceled.getStatus());
    }
}
