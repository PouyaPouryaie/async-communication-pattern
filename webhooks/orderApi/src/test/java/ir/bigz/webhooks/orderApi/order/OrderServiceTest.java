package ir.bigz.webhooks.orderApi.order;

import ir.bigz.webhooks.orderApi.domain.Order;
import ir.bigz.webhooks.orderApi.domain.OrderStatus;
import ir.bigz.webhooks.orderApi.domain.Product;
import ir.bigz.webhooks.orderApi.exception.InsufficientStockException;
import ir.bigz.webhooks.orderApi.exception.ProductNotFoundException;
import ir.bigz.webhooks.orderApi.payment.PaymentClient;
import ir.bigz.webhooks.orderApi.payment.PaymentInitiationResult;
import ir.bigz.webhooks.orderApi.repository.OrderRepository;
import ir.bigz.webhooks.orderApi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PaymentClient paymentClient;

    private OrderService orderService;

    @Test
    void createOrderComputesTotalAndInitiatesPayment() {
        orderService = new OrderService(orderRepository, productRepository, paymentClient);
        Product product = new Product();
        product.setId(1L);
        product.setName("Wireless Mouse");
        product.setPrice(new BigDecimal("20.00"));
        product.setStockQuantity(10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        AtomicReference<Order> savedOrder = new AtomicReference<>();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(UUID.randomUUID());
            }
            savedOrder.set(order);
            return order;
        });
        when(orderRepository.findById(any(UUID.class))).thenAnswer(invocation -> Optional.of(savedOrder.get()));
        when(paymentClient.initiatePayment(anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(new PaymentInitiationResult("payment-1", "PENDING"));

        OrderResponse response = orderService.createOrder(new CreateOrderRequest("customer@example.com", 1L, 3));

        assertThat(response.totalAmount()).isEqualByComparingTo("60.00");
        assertThat(response.status()).isEqualTo(OrderStatus.PAYMENT_PROCESSING);
        assertThat(response.currency()).isEqualTo("USD");
    }

    @Test
    void createOrderRejectsUnknownProduct() {
        orderService = new OrderService(orderRepository, productRepository, paymentClient);
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest("customer@example.com", 99L, 1)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void createOrderRejectsInsufficientStock() {
        orderService = new OrderService(orderRepository, productRepository, paymentClient);
        Product product = new Product();
        product.setId(1L);
        product.setPrice(new BigDecimal("20.00"));
        product.setStockQuantity(1);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest("customer@example.com", 1L, 5)))
                .isInstanceOf(InsufficientStockException.class);
    }
}
