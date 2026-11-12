package ir.bigz.messagequeue.consumer.application;

import ir.bigz.messagequeue.consumer.domain.InvalidOrderEventException;
import ir.bigz.messagequeue.consumer.domain.OrderEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessOrderEventUseCaseTest {

	private static final Instant OCCURRED_AT = Instant.parse("2026-11-11T00:00:00Z");

	@Test
	void rejectsEventWithBlankCorrelationId() {
		var useCase = new ProcessOrderEventUseCase(mock(ProcessedEventLedger.class));

		assertThatThrownBy(() -> useCase.process(new OrderEvent(" ", OCCURRED_AT)))
				.isInstanceOf(InvalidOrderEventException.class);
	}

	@Test
	void skipsDuplicateEventAlreadyInLedger() {
		var ledger = mock(ProcessedEventLedger.class);
		when(ledger.recordIfAbsent("order-00001")).thenReturn(false);
		var useCase = new ProcessOrderEventUseCase(ledger);

		assertThatCode(() -> useCase.process(new OrderEvent("order-00001", OCCURRED_AT)))
				.doesNotThrowAnyException();
		verify(ledger).recordIfAbsent("order-00001");
	}

}
