package ir.bigz.messagequeue.producer.infrastructure.persistence;

import ir.bigz.messagequeue.producer.application.OrderNumberSequence;
import ir.bigz.messagequeue.producer.domain.OrderNumberExhaustedException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class JdbcOrderNumberSequence implements OrderNumberSequence {

	private static final long LAST_ORDER_NUMBER = 99_999L;

	// One atomic statement: creates the row on first use, then increments it.
	private static final String NEXT_VALUE_SQL = """
			INSERT INTO order_sequence (id, last_value) VALUES (1, 1)
			ON CONFLICT (id) DO UPDATE SET last_value = order_sequence.last_value + 1
			RETURNING last_value
			""";

	private final JdbcClient jdbcClient;

	public JdbcOrderNumberSequence(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public String next() {
		long value = jdbcClient.sql(NEXT_VALUE_SQL).query(Long.class).single();
		if (value > LAST_ORDER_NUMBER) {
			throw new OrderNumberExhaustedException(value);
		}
		return String.format("order-%05d", value);
	}

}
