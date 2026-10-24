# ADR-002: Transactional Outbox Pattern for Reliable Event Publishing

## Status
Accepted

## Context
When a client initiates a payment, the system must write the payment record to PostgreSQL and publish a `PaymentInitiated` event to Apache Kafka.
If we write to the database and then call Kafka directly in the HTTP request:
- A crash after DB commit but before Kafka send results in a "ghost payment" that is never validated or settled.
- Sending to Kafka first and DB commit failing causes money to be transferred for a non-existent payment.
Two-Phase Commit (2PC / XA) is slow, fragile, and unsupported by modern cloud brokers like Kafka.

## Decision
We implemented the **Transactional Outbox Pattern**:
1. Within a single local ACID database transaction, `payment-service` writes the `payments` record and inserts an `outbox_events` record.
2. A background scheduled poller (`OutboxPoller`) reads uncommitted outbox rows, publishes them to Kafka, and marks them `SENT`.
3. If Kafka is temporarily down, the HTTP call still succeeds and the event safely waits in the outbox table until Kafka recovers.

## Consequences
- **Positive**: Guaranteed at-least-once message delivery, no dual-write distributed inconsistencies, zero reliance on distributed transactions.
- **Trade-off**: Requires consumer deduplication because poller retries may produce duplicate messages if an ack is dropped.
