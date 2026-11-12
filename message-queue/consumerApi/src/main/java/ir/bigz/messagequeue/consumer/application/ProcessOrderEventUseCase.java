package ir.bigz.messagequeue.consumer.application;

import ir.bigz.messagequeue.consumer.domain.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProcessOrderEventUseCase {

	public void process(OrderEvent event) {
		log.info("Order event received: correlationId={}", event.correlationId());
	}

}
