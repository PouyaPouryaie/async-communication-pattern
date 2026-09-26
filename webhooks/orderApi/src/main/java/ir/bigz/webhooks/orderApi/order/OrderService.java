package ir.bigz.webhooks.orderApi.order;

import ir.bigz.webhooks.orderApi.domain.Order;
import ir.bigz.webhooks.orderApi.payment.PaymentClient;
import ir.bigz.webhooks.orderApi.payment.PaymentInitiationResult;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderSettlementService orderSettlementService;
    private final PaymentClient paymentClient;

    public OrderService(OrderSettlementService orderSettlementService, PaymentClient paymentClient) {
        this.orderSettlementService = orderSettlementService;
        this.paymentClient = paymentClient;
    }

    /**
     * Creates the order, then starts payment. The pending order is
     * persisted and committed before the (blocking) payment-application
     * call is made, so a slow or failing call to the payment application
     * does not hold a database transaction open.
     */
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = orderSettlementService.createPendingOrder(request);
        PaymentInitiationResult result = paymentClient.initiatePayment(
                order.getId().toString(), order.getTotalAmount(), order.getCurrency());
        Order updated = orderSettlementService.markPaymentProcessing(order.getId(), result.paymentId());
        return OrderResponse.from(updated);
    }
}
