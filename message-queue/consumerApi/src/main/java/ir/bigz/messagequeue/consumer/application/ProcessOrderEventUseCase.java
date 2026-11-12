package ir.bigz.messagequeue.consumer.application;

import ir.bigz.messagequeue.consumer.domain.InvalidOrderEventException;
import ir.bigz.messagequeue.consumer.domain.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ProcessOrderEventUseCase {

	private final ProcessedEventLedger processedEventLedger;

	public ProcessOrderEventUseCase(ProcessedEventLedger processedEventLedger) {
		this.processedEventLedger = processedEventLedger;
	}

	@Transactional
	public void process(OrderEvent event) {
		if (event.correlationId() == null || event.correlationId().isBlank()) {
			throw new InvalidOrderEventException("Order event has no correlationId");
		}
		if (!processedEventLedger.recordIfAbsent(event.correlationId())) {
			log.info("Duplicate order event skipped: correlationId={}", event.correlationId());
			return;
		}
		log.info("Order event received: correlationId={}", event.correlationId());
	}

}
