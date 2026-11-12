package ir.bigz.messagequeue.producer.domain;

public class OrderPublishException extends RuntimeException {

	public OrderPublishException(String message) {
		super(message);
	}

	public OrderPublishException(String message, Throwable cause) {
		super(message, cause);
	}

}
