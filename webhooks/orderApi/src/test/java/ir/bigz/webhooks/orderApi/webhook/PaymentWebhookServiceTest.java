package ir.bigz.webhooks.orderApi.webhook;

import ir.bigz.webhooks.orderApi.domain.Order;
import ir.bigz.webhooks.orderApi.domain.OrderStatus;
import ir.bigz.webhooks.orderApi.domain.Product;
import ir.bigz.webhooks.orderApi.repository.OrderRepository;
import ir.bigz.webhooks.orderApi.repository.ProcessedWebhookEventRepository;
import ir.bigz.webhooks.orderApi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

    @Mock
    private ProcessedWebhookEventRepository processedWebhookEventRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private EmailService emailService;

    private PaymentWebhookService paymentWebhookService;

    @Test
    void skipsProcessingWhenServiceLayerIdempotencyCheckFindsExistingEvent() {
        paymentWebhookService = new PaymentWebhookService(
                processedWebhookEventRepository, orderRepository, productRepository, emailService);
        when(processedWebhookEventRepository.existsByPaymentId("payment-1")).thenReturn(true);

        paymentWebhookService.processWebhook(samplePayload("payment-1", "SUCCEEDED"));

        verify(orderRepository, never()).findById(any());
        verify(processedWebhookEventRepository, never()).insertIfAbsent(anyString(), any());
    }

    @Test
    void skipsProcessingWhenDatabaseLayerInsertFindsConcurrentDuplicate() {
        paymentWebhookService = new PaymentWebhookService(
                processedWebhookEventRepository, orderRepository, productRepository, emailService);
        when(processedWebhookEventRepository.existsByPaymentId("payment-2")).thenReturn(false);
        when(processedWebhookEventRepository.insertIfAbsent(eq("payment-2"), any())).thenReturn(0);

        paymentWebhookService.processWebhook(samplePayload("payment-2", "SUCCEEDED"));

        verify(orderRepository, never()).findById(any());
    }

    @Test
    void succeededPaymentMarksOrderPaidDecrementsStockAndSendsEmail() {
        paymentWebhookService = new PaymentWebhookService(
                processedWebhookEventRepository, orderRepository, productRepository, emailService);
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setProductId(1L);
        order.setQuantity(2);
        order.setStatus(OrderStatus.PAYMENT_PROCESSING);

        Product product = new Product();
        product.setId(1L);
        product.setStockQuantity(10);

        when(processedWebhookEventRepository.existsByPaymentId("payment-3")).thenReturn(false);
        when(processedWebhookEventRepository.insertIfAbsent(eq("payment-3"), any())).thenReturn(1);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        paymentWebhookService.processWebhook(samplePayload("payment-3", "SUCCEEDED", orderId));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(product.getStockQuantity()).isEqualTo(8);
        verify(emailService).sendOrderConfirmation(order);
    }

    @Test
    void failedPaymentMarksOrderFailedAndDoesNotTouchStockOrEmail() {
        paymentWebhookService = new PaymentWebhookService(
                processedWebhookEventRepository, orderRepository, productRepository, emailService);
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setProductId(1L);
        order.setQuantity(2);
        order.setStatus(OrderStatus.PAYMENT_PROCESSING);

        when(processedWebhookEventRepository.existsByPaymentId("payment-4")).thenReturn(false);
        when(processedWebhookEventRepository.insertIfAbsent(eq("payment-4"), any())).thenReturn(1);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        paymentWebhookService.processWebhook(samplePayload("payment-4", "FAILED", orderId));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        verify(productRepository, never()).findById(any());
        verify(emailService, never()).sendOrderConfirmation(any());
    }

    private PaymentWebhookPayload samplePayload(String paymentId, String status) {
        return samplePayload(paymentId, status, UUID.randomUUID());
    }

    private PaymentWebhookPayload samplePayload(String paymentId, String status, UUID orderId) {
        return new PaymentWebhookPayload(
                paymentId, orderId.toString(), status, new BigDecimal("10.00"), "USD", Instant.now());
    }
}
