package ir.bigz.polling.orderPollingApi;

import ir.bigz.polling.orderPollingApi.domain.OrderStatus;
import ir.bigz.polling.orderPollingApi.domain.OrderType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    // Constructor injection is standard for Clean Architecture in Spring
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/init")
    public ResponseEntity<String> initializeOrder(@RequestParam int orderNumber, @RequestParam OrderType type) {
        int processingTime = orderService.initializeOrder(orderNumber, type);
        return ResponseEntity.ok("Initialized order with " + processingTime + "s processing time.");
    }

    @GetMapping("/{orderNumber}/short-poll")
    public ResponseEntity<OrderStatus> shortPoll(@PathVariable int orderNumber) {
        OrderStatus status = orderService.processShortPoll(orderNumber);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/{orderNumber}/long-poll")
    public ResponseEntity<OrderStatus> longPoll(@PathVariable int orderNumber) {
        OrderStatus status = orderService.processLongPoll(orderNumber);
        return ResponseEntity.ok(status);
    }
}
