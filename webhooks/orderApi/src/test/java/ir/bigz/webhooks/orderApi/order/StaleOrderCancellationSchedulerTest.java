package ir.bigz.webhooks.orderApi.order;

import ir.bigz.webhooks.orderApi.domain.Order;
import ir.bigz.webhooks.orderApi.domain.OrderStatus;
import ir.bigz.webhooks.orderApi.payment.PaymentCancellationResult;
import ir.bigz.webhooks.orderApi.payment.PaymentClient;
import ir.bigz.webhooks.orderApi.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaleOrderCancellationSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentClient paymentClient;

    @Test
    void marksOrderCanceledOnlyAfterPaymentApiConfirmsCancellation() {
        StaleOrderCancellationScheduler scheduler = new StaleOrderCancellationScheduler(orderRepository, paymentClient);
        Order order = staleOrder();
        when(orderRepository.findByStatusAndUpdatedAtBefore(eq(OrderStatus.PAYMENT_PROCESSING), any(Instant.class)))
                .thenReturn(List.of(order));
        when(paymentClient.cancelPayment(order.getId().toString(), order.getPaymentId()))
                .thenReturn(new PaymentCancellationResult("CANCELED"));

        scheduler.cancelStaleOrders();

        verify(orderRepository).markCanceledIfProcessing(
                eq(order.getId()), eq(order.getPaymentId()), any(Instant.class),
                eq(OrderStatus.PAYMENT_PROCESSING), eq(OrderStatus.CANCELED));
    }

    @Test
    void doesNotMarkOrderCanceledWhenPaymentApiDoesNotConfirmCancellation() {
        StaleOrderCancellationScheduler scheduler = new StaleOrderCancellationScheduler(orderRepository, paymentClient);
        Order order = staleOrder();
        when(orderRepository.findByStatusAndUpdatedAtBefore(eq(OrderStatus.PAYMENT_PROCESSING), any(Instant.class)))
                .thenReturn(List.of(order));
        when(paymentClient.cancelPayment(order.getId().toString(), order.getPaymentId()))
                .thenReturn(new PaymentCancellationResult("SUCCEEDED"));

        scheduler.cancelStaleOrders();

        verify(orderRepository, never()).markCanceledIfProcessing(
                any(), any(), any(), any(), any());
    }

    @Test
    void continuesAfterPaymentApiCallFails() {
        StaleOrderCancellationScheduler scheduler = new StaleOrderCancellationScheduler(orderRepository, paymentClient);
        Order order = staleOrder();
        when(orderRepository.findByStatusAndUpdatedAtBefore(eq(OrderStatus.PAYMENT_PROCESSING), any(Instant.class)))
                .thenReturn(List.of(order));
        when(paymentClient.cancelPayment(order.getId().toString(), order.getPaymentId()))
                .thenThrow(new RestClientException("paymentApi unavailable"));

        scheduler.cancelStaleOrders();

        verify(orderRepository, never()).markCanceledIfProcessing(
                any(), any(), any(), any(), any());
    }

    private Order staleOrder() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setPaymentId("payment-1");
        order.setStatus(OrderStatus.PAYMENT_PROCESSING);
        return order;
    }
}