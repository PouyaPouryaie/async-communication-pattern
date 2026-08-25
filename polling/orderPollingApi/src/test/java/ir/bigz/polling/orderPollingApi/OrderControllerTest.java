package ir.bigz.polling.orderPollingApi;

import ir.bigz.polling.orderPollingApi.domain.OrderAlreadyExistsException;
import ir.bigz.polling.orderPollingApi.domain.OrderNotFoundException;
import ir.bigz.polling.orderPollingApi.domain.OrderStatus;
import ir.bigz.polling.orderPollingApi.domain.OrderType;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {

    private OrderService orderService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService)).build();
    }

    @Test
    void initializeOrderReturnsProcessingTime() throws Exception {
        when(orderService.initializeOrder(12, OrderType.EXTERNAL)).thenReturn(8);

        mockMvc.perform(post("/api/v1/orders/init")
                        .param("orderNumber", "12")
                        .param("type", "EXTERNAL"))
                .andExpect(status().isOk())
                .andExpect(content().string("Initialized order with 8s processing time."));
    }

    @Test
    void initializeOrderPropagatesConflictForDuplicateOrder() throws Exception {
        when(orderService.initializeOrder(12, OrderType.EXTERNAL))
                .thenThrow(new OrderAlreadyExistsException("Order already exists"));

        mockMvc.perform(post("/api/v1/orders/init")
                        .param("orderNumber", "12")
                        .param("type", "EXTERNAL"))
                .andExpect(status().isConflict());
    }

    @Test
    void shortPollReturnsOrderStatus() throws Exception {
        when(orderService.processShortPoll(12)).thenReturn(OrderStatus.PENDING);

        mockMvc.perform(get("/api/v1/orders/12/short-poll"))
                .andExpect(status().isOk())
                .andExpect(content().string("\"PENDING\""));
    }

    @Test
    void shortPollPropagatesNotFoundAs404() throws Exception {
        when(orderService.processShortPoll(404))
                .thenThrow(new OrderNotFoundException("Order does not exist: 404"));

        mockMvc.perform(get("/api/v1/orders/404/short-poll"))
                .andExpect(status().isNotFound());
    }

    @Test
    void longPollReturnsTerminalStatus() throws Exception {
        when(orderService.processLongPoll(12)).thenReturn(OrderStatus.COMPLETED);

        mockMvc.perform(get("/api/v1/orders/12/long-poll"))
                .andExpect(status().isOk())
                .andExpect(content().string("\"COMPLETED\""));
    }

    @Test
    void longPollReturnsBadRequestForUnsupportedOrderType() throws Exception {
        when(orderService.processLongPoll(12))
                .thenThrow(new IllegalArgumentException("Long polling requires an EXTERNAL order type."));

        assertThrows(ServletException.class,
                () -> mockMvc.perform(get("/api/v1/orders/12/long-poll")));
    }
}

