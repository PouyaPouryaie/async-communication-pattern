package ir.bigz.messagequeue.consumer.infrastructure.messaging;

import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitListenerConfig {

	@Bean
	MessageConverter messageConverter() {
		return new JacksonJsonMessageConverter();
	}

}
