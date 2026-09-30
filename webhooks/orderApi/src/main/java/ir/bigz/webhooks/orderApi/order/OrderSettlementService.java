package ir.bigz.webhooks.orderApi.order;

import ir.bigz.webhooks.orderApi.domain.Order;
import ir.bigz.webhooks.orderApi.domain.OrderStatus;
import ir.bigz.webhooks.orderApi.domain.Product;
import ir.bigz.webhooks.orderApi.exception.InsufficientStockException;
import ir.bigz.webhooks.orderApi.exception.OrderNotFoundException;
import ir.bigz.webhooks.orderApi.exception.ProductNotFoundException;
import ir.bigz.webhooks.orderApi.repository.OrderRepository;
import ir.bigz.webhooks.orderApi.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class OrderSettlementService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private static final String CURRENCY = "USD";

    public OrderSettlementService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
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
