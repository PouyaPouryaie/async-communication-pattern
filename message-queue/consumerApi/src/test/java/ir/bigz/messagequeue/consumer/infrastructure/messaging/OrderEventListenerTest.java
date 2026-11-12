package ir.bigz.messagequeue.consumer.infrastructure.messaging;

import com.rabbitmq.client.Channel;
import ir.bigz.messagequeue.consumer.application.ProcessOrderEventUseCase;
import ir.bigz.messagequeue.consumer.domain.OrderEvent;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class OrderEventListenerTest {

	@Test
	void processesEventThenAcknowledgesDelivery() throws Exception {
		var processOrderEvent = mock(ProcessOrderEventUseCase.class);
		var channel = mock(Channel.class);
		var listener = new OrderEventListener(processOrderEvent);
		var event = new OrderEvent("order-00001", Instant.parse("2026-11-11T00:00:00Z"));

		listener.onOrderEvent(event, channel, 42L);

		InOrder inOrder = inOrder(processOrderEvent, channel);
		inOrder.verify(processOrderEvent).process(event);
		inOrder.verify(channel).basicAck(42L, false);
	}

}
