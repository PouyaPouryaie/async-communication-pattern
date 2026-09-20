package ir.bigz.webhooks.orderApi.webhook;

import ir.bigz.webhooks.orderApi.domain.Order;
import ir.bigz.webhooks.orderApi.domain.OrderStatus;
import ir.bigz.webhooks.orderApi.domain.Product;
import ir.bigz.webhooks.orderApi.exception.OrderNotFoundException;
import ir.bigz.webhooks.orderApi.exception.ProductNotFoundException;
import ir.bigz.webhooks.orderApi.repository.OrderRepository;
import ir.bigz.webhooks.orderApi.repository.ProcessedWebhookEventRepository;
import ir.bigz.webhooks.orderApi.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Applies the payment outcome reported by the payment application's webhook.
 * Guards against processing the same delivery twice at two layers:
 * <ol>
 *   <li>Service layer: an upfront existence check on the idempotency ledger.</li>
 *   <li>Database layer: an {@code INSERT ... ON CONFLICT DO NOTHING} against
 *       the ledger's unique constraint, which safely handles concurrent
 *       deliveries that both pass the service-layer check.</li>
 * </ol>
 */
@Service
public class PaymentWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookService.class);
    private static final String SUCCEEDED_STATUS = "SUCCEEDED";

    private final ProcessedWebhookEventRepository processedWebhookEventRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;

    public PaymentWebhookService(ProcessedWebhookEventRepository processedWebhookEventRepository,
                                  OrderRepository orderRepository,
                                  ProductRepository productRepository,
                                  EmailService emailService) {
        this.processedWebhookEventRepository = processedWebhookEventRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void processWebhook(PaymentWebhookPayload payload) {
        if (processedWebhookEventRepository.existsByPaymentId(payload.paymentId())) {
            log.info("Skipping already-processed webhook for payment {}", payload.paymentId());
            return;
        }

        int inserted = processedWebhookEventRepository.insertIfAbsent(payload.paymentId(), Instant.now());
        if (inserted == 0) {
            log.info("Skipping webhook for payment {}: marked processed by a concurrent delivery", payload.paymentId());
            return;
        }

        applyPaymentOutcome(payload);
    }

    private void applyPaymentOutcome(PaymentWebhookPayload payload) {
        UUID orderId = UUID.fromString(payload.orderId());
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        boolean succeeded = SUCCEEDED_STATUS.equalsIgnoreCase(payload.status());
        order.setStatus(succeeded ? OrderStatus.PAID : OrderStatus.PAYMENT_FAILED);
        order.setUpdatedAt(Instant.now());
        orderRepository.save(order);

        if (succeeded) {
            decrementStock(order);
            emailService.sendOrderConfirmation(order);
        }
    }

    private void decrementStock(Order order) {
        Product product = productRepository.findById(order.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(order.getProductId()));
        product.setStockQuantity(product.getStockQuantity() - order.getQuantity());
        productRepository.save(product);
    }
}
