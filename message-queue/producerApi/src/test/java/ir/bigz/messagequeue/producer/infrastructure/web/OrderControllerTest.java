package ir.bigz.messagequeue.producer.infrastructure.web;

import ir.bigz.messagequeue.producer.application.OrderEventPublisher;
import ir.bigz.messagequeue.producer.domain.OrderPublishException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderControllerTest {

	private static final String QUEUE = "order.events.queue";

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3-management");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AmqpAdmin amqpAdmin;

	@Autowired
	private JdbcClient jdbcClient;

	@MockitoSpyBean
	private OrderEventPublisher orderEventPublisher;

	@BeforeEach
	void resetState() {
		jdbcClient.sql("DELETE FROM orders").update();
		jdbcClient.sql("DELETE FROM order_sequence").update();
		amqpAdmin.purgeQueue(QUEUE);
	}

	@AfterEach
	void resetPublisher() {
		Mockito.reset(orderEventPublisher);
	}

	@Test
	void acceptsOrderAndReturnsCorrelationId() throws Exception {
		mockMvc.perform(post("/orders"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.correlationId").value("order-00001"));
	}

	@Test
	void returnsServiceUnavailableWhenPublishFails() throws Exception {
		doThrow(new OrderPublishException("broker nack")).when(orderEventPublisher).publish(any());

		mockMvc.perform(post("/orders"))
				.andExpect(status().isServiceUnavailable());
	}

}
