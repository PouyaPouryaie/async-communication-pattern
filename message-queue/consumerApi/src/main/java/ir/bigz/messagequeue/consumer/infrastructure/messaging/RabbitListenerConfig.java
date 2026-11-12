package ir.bigz.messagequeue.consumer.infrastructure.messaging;

import ir.bigz.messagequeue.consumer.domain.InvalidOrderEventException;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

import java.time.Duration;

@Configuration
public class RabbitListenerConfig {

	@Bean
	MessageConverter messageConverter() {
		return new JacksonJsonMessageConverter();
	}

	// Poison messages may not be valid JSON, so the DLQ listener receives the raw body.
	@Bean
	SimpleRabbitListenerContainerFactory deadLetterListenerContainerFactory(ConnectionFactory connectionFactory,
			SimpleRabbitListenerContainerFactoryConfigurer configurer) {
		var factory = new SimpleRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		factory.setMessageConverter(new SimpleMessageConverter());
		return factory;
	}

	@Bean
	RetryTemplate orderEventRetryTemplate() {
		return new RetryTemplate(RetryPolicy.builder()
				.maxRetries(2)
				.delay(Duration.ofSeconds(1))
				.multiplier(1)
				.excludes(InvalidOrderEventException.class)
				.build());
	}

}
