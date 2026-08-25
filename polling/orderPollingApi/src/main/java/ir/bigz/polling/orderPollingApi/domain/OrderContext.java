package ir.bigz.polling.orderPollingApi.domain;

import java.time.Duration;
import java.time.Instant;

public record OrderContext(int orderNumber, OrderType orderType, Instant initialTime, Duration processingTime) {
}
