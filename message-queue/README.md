# Order Events over RabbitMQ

A sample of publishing and consuming order events with RabbitMQ and Spring AMQP. Two independent Spring Boot 4.1 / Java 25 services communicate over RabbitMQ:

- **producerApi**: `POST /orders` assigns the next `correlationId` (`order-NNNNN`), stores the order as `PENDING`, publishes an `OrderEvent` with publisher confirms, and marks the order `PUBLISHED`.
- **consumerApi**: consumes `OrderEvent` messages with manual acks, retries transient failures, dead-letters unrecoverable ones, records processed events in a ledger, and alerts on poison messages.

## Topology

| Element | Name | Notes |
|---|---|---|
| Exchange | `order.events` | Topic, durable |
| Routing key | `order.created` | |
| Main queue | `order.events.queue` | Durable quorum queue, `x-dead-letter-exchange=order.events.dlx`, `x-message-ttl=60000` ms |
| Dead-letter exchange | `order.events.dlx` | Direct, durable |
| Dead-letter queue | `order.events.dlq` | Durable quorum queue, bound with `order.created` |

Both services declare this topology. The arguments must match exactly, or RabbitMQ rejects the declaration with `PRECONDITION_FAILED`.

## Running the sample

Prerequisites: Java 25, Maven, and Docker with Compose.

1. Start RabbitMQ and both databases:

   ```bash
   cd message-queue
   docker compose up -d
   ```

   The management UI is at <http://localhost:15672>.

2. Start the consumer (in one terminal):

   ```bash
   cd message-queue/consumerApi
   mvn spring-boot:run
   ```

3. Start the producer (in another terminal):

   ```bash
   cd message-queue/producerApi
   mvn spring-boot:run
   ```

4. Send an order:

   ```bash
   curl -i -X POST http://localhost:8083/orders
   ```

   A `202 Accepted` response carries the `correlationId`, such as `{"correlationId":"order-00001"}`. The consumer logs `Order event received: correlationId=order-00001`. If the broker does not confirm the publish within 5 seconds, the API returns `503 Service Unavailable` and the order stays `PENDING`.

Stop the stack with `docker compose down`. Add `-v` to also delete the database and RabbitMQ volumes.

## Configuration

Every setting has a default that matches the Compose file. Override them with environment variables.

| Variable | Default | Used by |
|---|---|---|
| `RABBITMQ_HOST` | `localhost` | both services |
| `RABBITMQ_PORT` | `5672` | both services |
| `RABBITMQ_USER` | `order_events_user` | both services |
| `RABBITMQ_PASSWORD` | `order_events_pass` | both services |
| `RABBITMQ_VHOST` | `order-events` | both services |
| `RABBITMQ_DEFAULT_USER`, `RABBITMQ_DEFAULT_PASS`, `RABBITMQ_DEFAULT_VHOST` | same as above | Compose, for the broker |
| `SERVER_PORT` | `8083` | producerApi |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | `localhost`, `5434`, `messagedb`, `message_user`, `message_pass` | producerApi |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | `localhost`, `5435`, `consumerdb`, `consumer_user`, `consumer_pass` | consumerApi |

The `RABBITMQ_DEFAULT_VHOST` value applies only on the first start of the broker. Changing it later requires `docker compose down -v`.

## Design notes

- **Order numbers.** The producer keeps a single-row `order_sequence` table. `getNext` is one atomic `INSERT ... ON CONFLICT DO UPDATE ... RETURNING` statement, and it fails with an explicit error after `order-99999`.
- **Order status.** Each order is saved as `PENDING` before publishing and set to `PUBLISHED` after a confirm ack. A `PENDING` row marks an order whose publish did not complete.
- **Manual acks.** The consumer acks only after a successful process. Unrecoverable events are rejected with `basicNack(tag, false, false)`, which dead-letters them without a requeue.
- **Retries.** Transient failures are retried inside the listener, 3 attempts in total (one first attempt and two retries, one second apart). `InvalidOrderEventException` is never retried. After the last attempt, the listener nacks the message without requeue, so it goes to the dead-letter queue. Boot's built-in retry interceptor is not used, because in manual-ack mode it does not nack after the retries run out.
- **Idempotency.** The consumer's `processed_order_events` ledger is keyed by `correlation_id`. A single `INSERT ... ON CONFLICT DO NOTHING` decides whether an event is new. The ledger write and the processing share one transaction, so a failed processing attempt rolls the ledger back and the retry can run again.
- **Poison messages.** A dead-lettered message is logged at ERROR and increments the counter `order.events.poison.messages`, exposed to Prometheus as `order_events_poison_messages_total`. The alert is the ERROR log line and the counter. No external channel is wired up.
- **Shutdown.** `spring.lifecycle.timeout-per-shutdown-phase` is 20 seconds, which gives in-flight listeners time to finish.

## Ordering

RabbitMQ does not guarantee ordering when consumer concurrency or prefetch is above 1. `consumerApi` uses concurrency 1 and prefetch 10, so the caveat applies. Failed and retried messages can be processed out of their original order relative to later messages in the same prefetch batch. Do not rely on ordering across order events in this sample.

## Known limitations

- The order save and the publish are not in one transaction. A crash after the `PENDING` save and before the publish leaves a `PENDING` order. The sample has no recovery job.
- Each `POST /orders` call consumes a sequence number even when the publish fails.
- An unroutable message returned by the broker is not handled. The topology routes every event in this sample, so this path does not occur.
- The consumer's business effect is a log line. The sample has no downstream system.
- DLQ replay, TLS, and Kubernetes are out of scope.
