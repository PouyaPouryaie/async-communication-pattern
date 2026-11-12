package ir.bigz.messagequeue.producer.domain;

public class OrderNumberExhaustedException extends RuntimeException {

	public OrderNumberExhaustedException(long lastValue) {
		super("Order number sequence exhausted at " + lastValue);
	}

}
