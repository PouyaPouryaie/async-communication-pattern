package ir.bigz.messagequeue.consumer.application;

import ir.bigz.messagequeue.consumer.domain.InvalidOrderEventException;
import ir.bigz.messagequeue.consumer.domain.OrderEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessOrderEventUseCaseTest {

	@Test
	void rejectsEventWithBlankCorrelationId() {
		var useCase = new ProcessOrderEventUseCase();

		assertThatThrownBy(() -> useCase.process(new OrderEvent(" ", Instant.parse("2026-11-11T00:00:00Z"))))
				.isInstanceOf(InvalidOrderEventException.class);
	}

}
