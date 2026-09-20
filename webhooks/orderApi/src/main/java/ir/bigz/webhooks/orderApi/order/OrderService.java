package ir.bigz.webhooks.orderApi.order;

import ir.bigz.webhooks.orderApi.domain.Order;
import ir.bigz.webhooks.orderApi.domain.OrderStatus;
import ir.bigz.webhooks.orderApi.domain.Product;
import ir.bigz.webhooks.orderApi.exception.InsufficientStockException;
import ir.bigz.webhooks.orderApi.exception.OrderNotFoundException;
import ir.bigz.webhooks.orderApi.exception.ProductNotFoundException;
import ir.bigz.webhooks.orderApi.payment.PaymentClient;
import ir.bigz.webhooks.orderApi.payment.PaymentInitiationResult;
import ir.bigz.webhooks.orderApi.repository.OrderRepository;
import ir.bigz.webhooks.orderApi.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {

    private static final String CURRENCY = "USD";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentClient paymentClient;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, PaymentClient paymentClient) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.paymentClient = paymentClient;
    }

    /**
     * Creates the order, then starts payment. The pending order is
     * persisted and committed before the (blocking) payment-application
     * call is made, so a slow or failing call to the payment application
     * does not hold a database transaction open.
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = createPendingOrder(request);
        PaymentInitiationResult result = paymentClient.initiatePayment(
                order.getId().toString(), order.getTotalAmount(), order.getCurrency());
        Order updated = markPaymentProcessing(order.getId(), result.paymentId());
        return OrderResponse.from(updated);
    }

    @Transactional
    Order createPendingOrder(CreateOrderRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));
        if (product.getStockQuantity() < request.quantity()) {
            throw new InsufficientStockException(request.productId(), request.quantity(), product.getStockQuantity());
        }

        Instant now = Instant.now();
        Order order = new Order();
        order.setCustomerEmail(request.customerEmail());
        order.setProductId(product.getId());
        order.setQuantity(request.quantity());
        order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(request.quantity())));
        order.setCurrency(CURRENCY);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return orderRepository.save(order);
    }

    @Transactional
    Order markPaymentProcessing(UUID orderId, String paymentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.setPaymentId(paymentId);
        order.setStatus(OrderStatus.PAYMENT_PROCESSING);
        order.setUpdatedAt(Instant.now());
        return orderRepository.save(order);
    }
}
