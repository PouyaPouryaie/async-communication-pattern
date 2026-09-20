package ir.bigz.webhooks.paymentApi.payment;

import ir.bigz.webhooks.paymentApi.domain.Payment;
import ir.bigz.webhooks.paymentApi.domain.PaymentStatus;
import ir.bigz.webhooks.paymentApi.repository.PaymentRepository;
import ir.bigz.webhooks.paymentApi.webhook.PaymentProcessedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulates a payment provider (e.g. Stripe/PayPal) processing a payment in
 * the background: a short random delay followed by a random success/failure
 * outcome. Runs asynchronously so {@link PaymentService#initiatePayment} can
 * return to the caller immediately, mirroring how a real payment provider
 * would not make the merchant's request wait for the payment to complete.
 */
@Component
public class PaymentProcessingSimulator {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessingSimulator.class);
    private static final Random RANDOM = new Random();

    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final int minDelaySeconds;
    private final int maxDelaySeconds;
    private final double successRate;

    public PaymentProcessingSimulator(PaymentRepository paymentRepository,
                                       ApplicationEventPublisher eventPublisher,
                                       @Value("${app.simulation.min-delay-seconds:3}") int minDelaySeconds,
                                       @Value("${app.simulation.max-delay-seconds:6}") int maxDelaySeconds,
                                       @Value("${app.simulation.success-rate:0.85}") double successRate) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.minDelaySeconds = minDelaySeconds;
        this.maxDelaySeconds = maxDelaySeconds;
        this.successRate = successRate;
    }

    @Async
    public void simulate(UUID paymentId) {
        try {
            long delaySeconds = ThreadLocalRandom.current().nextLong(minDelaySeconds, maxDelaySeconds + 1);
            Thread.sleep(delaySeconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        PaymentStatus outcome = RANDOM.nextDouble() < successRate ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED;
        applyOutcome(paymentId, outcome);
        eventPublisher.publishEvent(new PaymentProcessedEvent(paymentId));
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
