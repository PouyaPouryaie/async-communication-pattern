package ir.bigz.webhooks.paymentApi.webhook;

import ir.bigz.webhooks.paymentApi.domain.MerchantRegistration;
import ir.bigz.webhooks.paymentApi.domain.Payment;
import ir.bigz.webhooks.paymentApi.repository.MerchantRegistrationRepository;
import ir.bigz.webhooks.paymentApi.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to a settled payment by building the webhook payload and handing it
 * off to {@link WebhookSender} for signed, retried delivery to the merchant.
 */
@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final PaymentRepository paymentRepository;
    private final MerchantRegistrationRepository merchantRegistrationRepository;
    private final WebhookSender webhookSender;

    public PaymentEventListener(PaymentRepository paymentRepository,
                                 MerchantRegistrationRepository merchantRegistrationRepository,
                                 WebhookSender webhookSender) {
        this.paymentRepository = paymentRepository;
        this.merchantRegistrationRepository = merchantRegistrationRepository;
        this.webhookSender = webhookSender;
    }

    @EventListener
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        Payment payment = paymentRepository.findById(event.paymentId())
                .orElseThrow(() -> new IllegalStateException("Payment " + event.paymentId() + " not found"));
        MerchantRegistration merchant = merchantRegistrationRepository.findById(payment.getMerchantId())
                .orElseThrow(() -> new IllegalStateException("Merchant " + payment.getMerchantId() + " not found"));

        WebhookPayload payload = new WebhookPayload(
                payment.getId(),
                payment.getOrderId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getUpdatedAt());

        try {
            webhookSender.deliver(merchant, payload);
        } catch (Exception e) {
            log.error("Webhook delivery failed for payment {}: {}", payment.getId(), e.getMessage());
        }
    }
}
