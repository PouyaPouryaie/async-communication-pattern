package ir.bigz.messagequeue.producer.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderEventsTopologyConfig {

	static final String EXCHANGE = "order.events";
	static final String QUEUE = "order.events.queue";
	static final String DEAD_LETTER_EXCHANGE = "order.events.dlx";
	static final String DEAD_LETTER_QUEUE = "order.events.dlq";
	static final String ROUTING_KEY = "order.created";
	static final int MESSAGE_TTL_MS = 60_000;

	@Bean
	TopicExchange orderEventsExchange() {
		return new TopicExchange(EXCHANGE, true, false);
	}

	@Bean
	Queue orderEventsQueue() {
		return QueueBuilder.durable(QUEUE)
				.quorum()
				.deadLetterExchange(DEAD_LETTER_EXCHANGE)
				.ttl(MESSAGE_TTL_MS)
				.build();
	}

	@Bean
	Binding orderEventsBinding(Queue orderEventsQueue, TopicExchange orderEventsExchange) {
		return BindingBuilder.bind(orderEventsQueue).to(orderEventsExchange).with(ROUTING_KEY);
	}

	@Bean
	DirectExchange orderEventsDeadLetterExchange() {
		return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
	}

	@Bean
	Queue orderEventsDeadLetterQueue() {
		return QueueBuilder.durable(DEAD_LETTER_QUEUE).quorum().build();
	}

	// Dead-lettered messages keep their original routing key, so the DLQ binds the same key.
	@Bean
	Binding orderEventsDeadLetterBinding(Queue orderEventsDeadLetterQueue, DirectExchange orderEventsDeadLetterExchange) {
		return BindingBuilder.bind(orderEventsDeadLetterQueue).to(orderEventsDeadLetterExchange).with(ROUTING_KEY);
	}

}
