package ir.bigz.polling.orderPollingApi;

import ir.bigz.polling.orderPollingApi.domain.OrderAlreadyExistsException;
import ir.bigz.polling.orderPollingApi.domain.OrderContext;
import ir.bigz.polling.orderPollingApi.domain.OrderNotFoundException;
import ir.bigz.polling.orderPollingApi.domain.OrderStatus;
import ir.bigz.polling.orderPollingApi.domain.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderServiceTest {

    private OrderService orderService;
    private Map<Integer, OrderContext> orderRepository;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        orderService = new OrderService();
        Field repositoryField = OrderService.class.getDeclaredField("orderRepository");
        repositoryField.setAccessible(true);
        orderRepository = (Map<Integer, OrderContext>) repositoryField.get(orderService);
    }

    @Test
    void initializeOrderRegistersOrderAndReturnsProcessingTimeInConfiguredRange() {
        int processingTime = orderService.initializeOrder(1001, OrderType.EXTERNAL);

        assertTrue(processingTime >= 5 && processingTime <= 20);
        assertEquals(OrderStatus.PENDING, orderService.processShortPoll(1001));
    }

    @Test
    void initializeOrderRejectsDuplicateOrderNumber() {
        orderService.initializeOrder(1002, OrderType.INTERNAL);

        OrderAlreadyExistsException exception = assertThrows(
                OrderAlreadyExistsException.class,
                () -> orderService.initializeOrder(1002, OrderType.EXTERNAL));

        assertTrue(exception.getMessage().contains("1002"));
    }

    @Test
    void shortPollReturnsNotFoundForUnknownOrder() {
        OrderNotFoundException exception = assertThrows(
                OrderNotFoundException.class,
                () -> orderService.processShortPoll(404));

        assertTrue(exception.getMessage().contains("404"));
    }

    @Test
    void shortPollReturnsTerminalStatusAndRemovesCompletedOrder() {
        putOrder(1003, OrderType.INTERNAL, Instant.now().minusSeconds(2), Duration.ofSeconds(1));

        OrderStatus status = orderService.processShortPoll(1003);

        assertTrue(status == OrderStatus.COMPLETED || status == OrderStatus.REJECTED);
        assertThrows(OrderNotFoundException.class, () -> orderService.processShortPoll(1003));
    }

    @Test
    void longPollRejectsNonExternalOrders() {
        putOrder(1004, OrderType.INTERNAL, Instant.now(), Duration.ofSeconds(1));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.processLongPoll(1004));

        assertEquals("Long polling requires an EXTERNAL order type.", exception.getMessage());
    }

    @Test
    void longPollReturnsNotFoundForUnknownOrder() {
        assertThrows(OrderNotFoundException.class, () -> orderService.processLongPoll(405));
    }

    @Test
    void longPollCompletesAlreadyElapsedExternalOrderAndRemovesIt() {
        putOrder(1005, OrderType.EXTERNAL, Instant.now().minusSeconds(2), Duration.ofSeconds(1));

        OrderStatus status = processWithTransientBackendRetry();

        assertTrue(status == OrderStatus.COMPLETED || status == OrderStatus.REJECTED);
        assertThrows(OrderNotFoundException.class, () -> orderService.processLongPoll(1005));
    }

    private OrderStatus processWithTransientBackendRetry() {
        int orderNumber = 1005;
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                return orderService.processLongPoll(orderNumber);
            } catch (RuntimeException exception) {
                lastFailure = exception;
                putOrder(orderNumber, OrderType.EXTERNAL, Instant.now().minusSeconds(2), Duration.ofSeconds(1));
            }
        }
        throw new AssertionError("The simulated backend failed on every retry", lastFailure);
    }

    private void putOrder(int orderNumber, OrderType type, Instant initialTime, Duration processingTime) {
        orderRepository.put(orderNumber, new OrderContext(orderNumber, type, initialTime, processingTime));
    }
}


