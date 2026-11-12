package ir.bigz.messagequeue.consumer.infrastructure.messaging;

import com.rabbitmq.client.Channel;
import ir.bigz.messagequeue.consumer.application.ProcessOrderEventUseCase;
import ir.bigz.messagequeue.consumer.domain.InvalidOrderEventException;
import ir.bigz.messagequeue.consumer.domain.OrderEvent;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OrderEventListenerTest {

	@Test
	void processesEventThenAcknowledgesDelivery() throws Exception {
		var processOrderEvent = mock(ProcessOrderEventUseCase.class);
		var channel = mock(Channel.class);
		var listener = new OrderEventListener(processOrderEvent, noRetry());
		var event = new OrderEvent("order-00001", Instant.parse("2026-11-11T00:00:00Z"));

		listener.onOrderEvent(event, channel, 42L);

		InOrder inOrder = inOrder(processOrderEvent, channel);
		inOrder.verify(processOrderEvent).process(event);
		inOrder.verify(channel).basicAck(42L, false);
	}

	@Test
	void rejectsUnrecoverableEventWithoutRequeue() throws Exception {
		var processOrderEvent = mock(ProcessOrderEventUseCase.class);
		doThrow(new InvalidOrderEventException("blank correlationId")).when(processOrderEvent).process(any());
		var channel = mock(Channel.class);
		var listener = new OrderEventListener(processOrderEvent, noRetry());

		listener.onOrderEvent(new OrderEvent(" ", Instant.parse("2026-11-11T00:00:00Z")), channel, 42L);

		verify(channel).basicNack(42L, false, false);
		verify(channel, never()).basicAck(anyLong(), anyBoolean());
	}

	private static RetryTemplate noRetry() {
		return new RetryTemplate(RetryPolicy.withMaxRetries(0));
	}

}
