package ir.bigz.messagequeue.consumer.domain;

public class InvalidOrderEventException extends RuntimeException {

	public InvalidOrderEventException(String message) {
		super(message);
	}

}
