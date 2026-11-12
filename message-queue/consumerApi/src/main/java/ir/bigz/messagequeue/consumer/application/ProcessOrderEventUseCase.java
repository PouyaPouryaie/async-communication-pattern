package ir.bigz.messagequeue.consumer.application;

import ir.bigz.messagequeue.consumer.domain.InvalidOrderEventException;
import ir.bigz.messagequeue.consumer.domain.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProcessOrderEventUseCase {

	public void process(OrderEvent event) {
		if (event.correlationId() == null || event.correlationId().isBlank()) {
			throw new InvalidOrderEventException("Order event has no correlationId");
		}
		log.info("Order event received: correlationId={}", event.correlationId());
	}

}
