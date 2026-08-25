# Async by Design: Patterns for Modern Distributed Systems

This repository contains the runnable examples for the **Async by Design** technical series. Each project demonstrates one asynchronous communication pattern in a small Spring and Angular application, with the implementation, tests, and local setup kept together.

The examples are intended for mid-to-senior software engineers and solution architects who want to compare communication patterns by behavior, operational trade-offs, and service boundaries.

## Repository Map

| Episode | Pattern | Project | Status |
| --- | --- | --- | --- |
| EP-01 | Foundations: synchronous vs asynchronous communication | Planned | Planned |
| EP-02 | Short polling and long polling | [`polling/`](polling/) | Available |
| EP-03 | Webhooks | Planned | Planned |
| EP-04 | Message queues: RabbitMQ and Kafka | Planned | Planned |
| EP-05 | Server-Sent Events (SSE) | Planned | Planned |
| EP-06 | WebSockets | Planned | Planned |
| EP-07 | Decision framework | Planned | Planned |

New pattern projects will be added as separate top-level directories and linked from this table. The directory name should describe the pattern and each project should include its own README with architecture, request flow, runnable examples, and limitations.

**Takeaway:** The root README maps the series; each project README explains how to run and understand one pattern.

## Available Project: Polling

The [`polling/`](polling/) project is the completed first implementation. It models an order-processing workflow in which the initial request starts work and a client later checks its status.

It demonstrates:

- **Short polling:** the Angular client sends a new request every two seconds and receives the current status immediately.
- **Long polling:** the Spring server holds a request for up to eight seconds and returns when processing finishes or the wait window expires.
- An in-memory order store and simulated processing duration.
- An Angular standalone component with RxJS polling and subscription cleanup.
- A Spring MVC REST API with Java virtual threads and `StructuredTaskScope` for the long-polling wait.

Read the complete setup and API guide in [`polling/README.md`](polling/README.md).

```text
User
	|
	v
Angular client (localhost:4200)
	|  POST /init
	|  GET  /short-poll or /long-poll
	v
Spring Boot API (localhost:8080)
	|
	v
Order service -> in-memory order store
```

**Takeaway:** Polling is useful when work outlives the initiating HTTP request and a push connection is unnecessary or unavailable.

## Series Scope

The series compares these patterns across the same practical dimensions:

- How work is initiated, observed, retried, and completed.
- Internal service-to-service communication versus external partner or public API communication.
- Authentication, trust boundaries, validation, and failure handling.
- Network topology, API Gateway mediation, private connectivity, and firewall or NAT constraints.
- Scalability, resource usage, observability, compliance, and data boundaries.
- Kubernetes deployment concerns where infrastructure affects the pattern.

The examples use Java with Spring Boot on the server and TypeScript with Angular and RxJS on the client. The series baseline is Java 17+, Spring Boot 3.x, TypeScript 5.x, Angular 17+, and RxJS 7.x. Individual projects may temporarily use newer or older versions while the examples are being built; their project README is the source of truth for exact prerequisites.

**Takeaway:** Pattern selection depends on delivery semantics and operational context, not only on implementation convenience.

## Planned Pattern Coverage

| Pattern | Primary concepts |
| --- | --- |
| Foundations | Sync versus async execution, latency, coupling, and the pattern landscape |
| Polling | Short poll, long poll, backoff, request pressure, and thundering herd behavior |
| Webhooks | Registration, signed delivery, retries, idempotency, dead-letter handling, and fan-out |
| Message queues | RabbitMQ and Kafka, consumer behavior, poison messages, and hot/warm/cold storage |
| SSE | One-way event streams, replay with `Last-Event-ID`, scaling, and graceful shutdown |
| WebSockets | Bidirectional communication, reconnect replay, security hardening, and degradation paths |
| Decision framework | Pattern matrix, decision tree, hybrid architectures, service boundary, and anti-patterns |

## Common Project Conventions

Each episode should provide:

1. A conceptual explanation and a text-based flow diagram.
2. An explicit **Internal vs External Service Communication** section covering auth, trust, topology, and compliance.
3. Complete, runnable Java/Spring Boot and Angular/TypeScript examples where code is required.
4. Pros and cons, real-world operational considerations, and key takeaways.
5. A gap-discovery note recording uncovered edge cases and questions for later revisions.

Code examples favor constructor injection, records for suitable DTOs, explicit error handling, strict TypeScript types, Angular standalone components, and RxJS cleanup with `takeUntilDestroyed()` where supported by the project version.

## Running an Example

Each project is independently runnable. Start with the project-specific README because prerequisites and commands can differ between episodes. For the current Polling example:

```bash
cd polling/orderPollingApi
./mvnw spring-boot:run
```

In a second terminal:

```bash
cd polling/frontend
npm install
npm start
```

Then open `http://localhost:4200`. The backend API runs at `http://localhost:8080`.

**Takeaway:** Run examples from their own project directory so version-specific setup remains explicit.

## Series Progress

- [ ] EP-01 — Foundations: Sync vs Async
- [x] EP-02 — Polling
- [ ] EP-03 — Webhooks
- [ ] EP-04 — Message Queues
- [ ] EP-05 — Server-Sent Events (SSE)
- [ ] EP-06 — WebSockets
- [ ] EP-07 — Decision Framework

The detailed episode tracker, publishing cadence, and revision notes are maintained with the series planning material and will be reflected here as new runnable projects land.
