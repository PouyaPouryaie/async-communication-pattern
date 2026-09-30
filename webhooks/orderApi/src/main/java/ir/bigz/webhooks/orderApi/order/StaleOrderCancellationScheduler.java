package ir.bigz.webhooks.orderApi.order;

import ir.bigz.webhooks.orderApi.domain.Order;
import ir.bigz.webhooks.orderApi.domain.OrderStatus;
import ir.bigz.webhooks.orderApi.payment.PaymentCancellationResult;
import ir.bigz.webhooks.orderApi.payment.PaymentClient;
import ir.bigz.webhooks.orderApi.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;

@Component
public class StaleOrderCancellationScheduler {

    private static final Logger log = LoggerFactory.getLogger(StaleOrderCancellationScheduler.class);
    private static final Duration PAYMENT_TIMEOUT = Duration.ofMinutes(10);

    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;

    public StaleOrderCancellationScheduler(OrderRepository orderRepository, PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.paymentClient = paymentClient;
    }

    @Scheduled(fixedRate = 300_000)
    public void cancelStaleOrders() {
        Instant cutoff = Instant.now().minus(PAYMENT_TIMEOUT);
        orderRepository.findByStatusAndUpdatedAtBefore(OrderStatus.PAYMENT_PROCESSING, cutoff)
                .forEach(this::cancelIfPaymentConfirmed);
    }

    private void cancelIfPaymentConfirmed(Order order) {
        try {
            PaymentCancellationResult result = paymentClient.cancelPayment(order.getId().toString(), order.getPaymentId());
            if (!"CANCELED".equalsIgnoreCase(result.status())) {
                log.warn("Payment {} for stale order {} was not canceled (status {})",
                        order.getPaymentId(), order.getId(), result.status());
                return;
            }

            int updated = orderRepository.markCanceledIfProcessing(
                    order.getId(), order.getPaymentId(), Instant.now(),
                    OrderStatus.PAYMENT_PROCESSING, OrderStatus.CANCELED);
            if (updated == 1) {
                log.info("Canceled stale order {} after payment {} was canceled",
                        order.getId(), order.getPaymentId());
            }
        } catch (RestClientException exception) {
            log.error("Could not cancel payment {} for stale order {}: {}",
                    order.getPaymentId(), order.getId(), exception.getMessage());
        }
    }
}