package ir.bigz.messagequeue.consumer.infrastructure.messaging;

import ir.bigz.messagequeue.consumer.domain.InvalidOrderEventException;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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
