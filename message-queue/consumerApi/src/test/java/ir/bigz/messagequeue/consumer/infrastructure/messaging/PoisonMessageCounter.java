package ir.bigz.messagequeue.consumer.infrastructure.messaging;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

final class PoisonMessageCounter {

	private static final String NAME = "order.events.poison.messages";

	private PoisonMessageCounter() {
	}

	static double count(MeterRegistry meterRegistry) {
		Counter counter = meterRegistry.find(NAME).counter();
		return counter == null ? 0 : counter.count();
	}

	static void awaitIncrement(MeterRegistry meterRegistry, double baseline) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 5_000;
		while (count(meterRegistry) < baseline + 1 && System.currentTimeMillis() < deadline) {
			Thread.sleep(50);
		}
	}

}
