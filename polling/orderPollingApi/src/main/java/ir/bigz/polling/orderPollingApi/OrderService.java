package ir.bigz.polling.orderPollingApi;

import ir.bigz.polling.orderPollingApi.domain.OrderAlreadyExistsException;
import ir.bigz.polling.orderPollingApi.domain.OrderContext;
import ir.bigz.polling.orderPollingApi.domain.OrderNotFoundException;
import ir.bigz.polling.orderPollingApi.domain.OrderStatus;
import ir.bigz.polling.orderPollingApi.domain.OrderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stores orders in memory and exposes short- and long-polling processing models.
 *
 * <p>The repository is shared by concurrent HTTP requests, so it uses a
 * {@link ConcurrentHashMap}. Long polling is limited to a bounded window; a
 * request that reaches that limit receives {@link OrderStatus#PENDING} and may
 * poll the same order again.</p>
 */
@Service
@Slf4j
public class OrderService {

    private final Map<Integer, OrderContext> orderRepository = new ConcurrentHashMap<>();
    private static final Duration LONG_POLLING_TIMEOUT = Duration.ofSeconds(8);
    private final ThreadFactory longPollVirtualThreadFactory = Thread.ofVirtual()
            .name("order-long-poll-", 0) // Generates readable thread names: order-long-poll-0, order-long-poll-1...
            .factory();

    /**
     * Registers a new order and assigns it a simulated processing duration.
     *
     * @param orderNumber unique number of the order
     * @param type order category, which also determines whether long polling is allowed
     * @return the simulated processing duration in seconds
     * @throws OrderAlreadyExistsException if an order with {@code orderNumber} is already registered
     */
    public int initializeOrder(int orderNumber, OrderType type) {
        if (orderRepository.containsKey(orderNumber)) {
            throw new OrderAlreadyExistsException("Order with number " + orderNumber + " already exists.");
        }

        int randomSeconds = ThreadLocalRandom.current().nextInt(5, 21);

        OrderContext orderContext = new OrderContext(orderNumber, type, Instant.now(), Duration.ofSeconds(randomSeconds));

        log.info("Initialized order {} of type {} with processing time {} seconds created at {}",
                orderNumber, type, randomSeconds, orderContext.initialTime());

        orderRepository.put(orderNumber, orderContext);
        return randomSeconds;
    }

    /**
     * Checks an order once without blocking the request thread.
     *
     * <p>While processing is still in progress, this method returns
     * {@link OrderStatus#PENDING}. Once processing time has elapsed, it returns
     * a terminal status and removes the order from the repository.</p>
     *
     * @param orderNumber number of the order to check
     * @return {@code PENDING} while processing, otherwise {@code COMPLETED} or {@code REJECTED}
     * @throws OrderNotFoundException if the order is not registered
     */
    public OrderStatus processShortPoll(int orderNumber) {
        OrderContext orderContext = fetchOrderOrThrow(orderNumber);

        Duration elapsedTime = Duration.between(orderContext.initialTime(), Instant.now());
        if (elapsedTime.compareTo(orderContext.processingTime()) >= 0) {
            OrderStatus finalStatus = generateRandomOutcome();
            orderRepository.remove(orderNumber);
            return finalStatus;
        }
        return OrderStatus.PENDING;
    }

    /**
     * Waits for an external order to finish, for at most
     * {@link #LONG_POLLING_TIMEOUT}.
     *
     * <p>The processing wait is forked onto a virtual thread so that the
     * request can wait without occupying a platform thread. The
     * {@code awaitAllSuccessfulOrThrow()} joiner is intentional: this
     * operation has one required child, and a child failure
     * must be propagated as a failed long-poll request rather than converted to
     * a successful or pending order status.</p>
     *
     * <p>The scope configuration establishes the operational policy around
     * that child: {@code withTimeout} bounds how long a client request can
     * wait, {@code withThreadFactory} uses named virtual threads for efficient
     * blocking and diagnostics, and {@code withName} makes the scope itself
     * identifiable in thread dumps and monitoring. If the timeout expires,
     * this method returns {@link OrderStatus#PENDING} and keeps the order in
     * the repository so a later poll can retry. The order is removed only
     * after the child completes successfully.</p>
     *
     * @param orderNumber number of the external order to process
     * @return a terminal status when processing completes, or {@code PENDING} when the wait times out
     * @throws OrderNotFoundException if the order is not registered
     * @throws IllegalArgumentException if the order is not {@link OrderType#EXTERNAL}
     * @throws RuntimeException if the wait is interrupted or the processing task fails
     */
    public OrderStatus processLongPoll(int orderNumber) {
        OrderContext context = fetchOrderOrThrow(orderNumber);

        // Long polling is part of the external-order contract in this demo.
        if (context.orderType() != OrderType.EXTERNAL) {
            throw new IllegalArgumentException("Long polling requires an EXTERNAL order type.");
        }

        log.info("Processing long poll for order {} of type {} with processing time {} seconds created at {}",
                orderNumber, context.orderType(), context.processingTime().getSeconds(), context.initialTime());

        // Configure the scope's lifetime, worker threads, and diagnostic name
        // instead of managing a thread pool and queue explicitly.
        try (var scope = StructuredTaskScope.open(
                StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow(),
                config -> config
                        .withTimeout(LONG_POLLING_TIMEOUT)
                        .withThreadFactory(longPollVirtualThreadFactory)
                        .withName("long-poll-scope-" + orderNumber))) {

            // The blocking wait runs on a virtual thread, allowing the request
            // to wait without occupying a platform-thread carrier.
            var processingTask = scope.fork(() -> simulateProcessingWait(context));

            // Wait for success, failure, or the configured timeout. Parking a
            // virtual thread does not occupy its carrier while it is waiting.
            scope.join();

            // Reaching this line means the task completed within the timeout.
            OrderStatus outcome = processingTask.get();

            // Keep timed-out or failed orders available; remove only a completed task.
            orderRepository.remove(orderNumber);

            log.info("Long poll for order {} completed with status: {}", orderNumber, outcome);

            return outcome;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted during long polling", e);
        } catch (Exception e) {
            // A timeout is a normal long-poll result: the order remains stored
            // so the client can issue another request later.
            if (e instanceof StructuredTaskScope.TimeoutException) {
                log.warn("Long poll for order {} timed out after {} seconds", orderNumber, LONG_POLLING_TIMEOUT.getSeconds());
                return OrderStatus.PENDING;
            }
            throw new RuntimeException("Underlying long-poll task failed", e);
        }
    }

    /**
     * Looks up an order and translates a missing entry into the domain exception
     * used by the REST layer.
     *
     * @param orderNumber number of the order to find
     * @return the registered order context
     * @throws OrderNotFoundException if no context exists for the number
     */
    private OrderContext fetchOrderOrThrow(int orderNumber) {
        OrderContext context = orderRepository.get(orderNumber);
        if (context == null) {
            throw new OrderNotFoundException("Order does not exist: " + orderNumber);
        }
        return context;
    }

    /**
     * Simulates backend processing by suspending the current virtual thread
     * for the context's remaining processing time.
     *
     * @param context order and timing information used by the simulation
     * @return a simulated completed or rejected status
     * @throws InterruptedException if the structured scope cancels the wait
     * @throws IllegalStateException when the simulated backend fails
     */
    private OrderStatus simulateProcessingWait(OrderContext context) throws InterruptedException {
        log.info("Started processing order {} on the long-poll worker", context.orderNumber());

        // Recalculate the remaining wait because some processing may have
        // elapsed between initialization and this long-poll request.
        Duration elapsed = Duration.between(context.initialTime(), Instant.now());
        Duration remainingWait = context.processingTime().minus(elapsed);

        if (!remainingWait.isNegative() && !remainingWait.isZero()) {
            // Sleeping parks the virtual thread instead of blocking a platform
            // thread while the simulated backend work is in progress.
            Thread.sleep(remainingWait);
        }

        // Simulate success, rejection, or an unexpected backend failure.
        int outcome = ThreadLocalRandom.current().nextInt(100);
        if (outcome < 5) {
            throw new IllegalStateException("Simulated backend database failure");
        }

        OrderStatus status = outcome < 50 ? OrderStatus.COMPLETED : OrderStatus.REJECTED;
        log.info("Finished processing order {} on the long-poll worker with status: {}",
                context.orderNumber(), status);
        return status;
    }

    /**
     * Generates a terminal status for the non-blocking polling path.
     *
     * @return either {@link OrderStatus#COMPLETED} or {@link OrderStatus#REJECTED}
     */
    private OrderStatus generateRandomOutcome() {
        return ThreadLocalRandom.current().nextBoolean() ? OrderStatus.COMPLETED : OrderStatus.REJECTED;
    }

}
