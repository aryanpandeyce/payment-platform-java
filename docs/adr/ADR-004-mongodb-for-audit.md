# ADR-004: MongoDB for Centralized Immutable Event Audit Logs

## Status
Accepted

## Context
Financial compliance standards require a permanent, unalterable audit trail recording every state change and domain event.
Relational tables with strict column schemas struggle to store disparate JSON event formats (`AccountCreated`, `PaymentInitiated`, `PaymentValidated`, `PaymentSettled`, `PaymentFailed`) without cumbersome JSON columns or complex polymorphism.

## Decision
We chose **MongoDB** for the `audit_logs` collection:
1. **Schema Flexibility**: Stores arbitrary domain event payloads in native BSON/JSON without schema migrations when new microservice event types are added.
2. **Append-Only Enforcement**: The service exposes query-only REST endpoints; no update or delete APIs are ever exposed, guaranteeing ledger immutability.
3. **Rich Secondary Indexing**: Efficiently queries historical transactions by `correlationId`, `aggregateId`, `eventType`, or timestamp range.

## Consequences
- **Positive**: Seamless event schema evolution, high write throughput for append operations, fast regulatory audit queries.
- **Trade-off**: Requires maintaining a separate MongoDB database instance alongside PostgreSQL relational engines.
