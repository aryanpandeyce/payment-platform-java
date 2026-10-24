# ADR-001: Why Apache Kafka for Microservice Choreography

## Status
Accepted

## Context
In our payment processing system, payment lifecycle operations span multiple autonomous services: `payment-service` coordinates transaction states, `fraud-service` evaluates risk limits and velocity, `account-service` executes balance debits and credits, and `notification-service` alerts users and logs audit events.
We evaluated synchronous REST/gRPC vs. asynchronous message brokering (RabbitMQ vs. Apache Kafka).

## Decision
We chose **Apache Kafka (KRaft mode)** as our event backbone.
1. **Partition-Ordered Streaming**: Kafka partitions preserve message order per payment (`key = paymentId`), ensuring events like `PaymentInitiated`, `PaymentValidated`, and `PaymentSettled` are consumed in exact chronological order.
2. **Decoupled Consumer Groups**: Multiple autonomous services consume the same stream independently at their own pace without impacting payment response times.
3. **High Throughput & Replayability**: Kafka retains events on disk, allowing new audit consumers or analytics workers to replay past events from offset 0.

## Consequences
- **Positive**: High throughput, fault isolation, broker resilience, replayable transaction stream.
- **Trade-off**: Requires eventual consistency handling, consumer deduplication, and dead-letter queue (DLT) management.
