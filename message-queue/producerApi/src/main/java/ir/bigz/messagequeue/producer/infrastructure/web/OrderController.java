package ir.bigz.messagequeue.producer.infrastructure.web;

import ir.bigz.messagequeue.producer.application.PublishOrderEventUseCase;
import ir.bigz.messagequeue.producer.domain.OrderPublishException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

	private final PublishOrderEventUseCase publishOrderEvent;

	public OrderController(PublishOrderEventUseCase publishOrderEvent) {
		this.publishOrderEvent = publishOrderEvent;
	}

	@PostMapping
	public ResponseEntity<OrderAcceptedResponse> create() {
		String correlationId = publishOrderEvent.publish();
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(new OrderAcceptedResponse(correlationId));
	}

	@ExceptionHandler(OrderPublishException.class)
	public ResponseEntity<Void> handlePublishFailure() {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
	}

}
