package ir.bigz.messagequeue.consumer.application;

public interface ProcessedEventLedger {

	boolean recordIfAbsent(String correlationId);

}
