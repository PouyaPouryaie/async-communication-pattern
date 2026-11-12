package ir.bigz.messagequeue.producer.infrastructure.messaging;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitPublishingConfig {

	@Bean
	MessageConverter messageConverter() {
		return new JacksonJsonMessageConverter();
	}

	@Bean
	RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
		var rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(messageConverter);
		rabbitTemplate.setMandatory(true);
		rabbitTemplate.setBeforePublishPostProcessors(message -> {
			message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
			return message;
		});
		return rabbitTemplate;
	}

}
