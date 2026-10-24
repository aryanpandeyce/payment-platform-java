# ADR-003: Client-Supplied Idempotency Keys for Transfer Deduplication

## Status
Accepted

## Context
In payment systems, network timeouts, cellular connection drops, or rapid double-clicks often cause clients to retry a `POST /api/payments` request.
Without deduplication, retried requests create duplicate payments and double-debit customer accounts.

## Decision
We enforce a mandatory UUID `Idempotency-Key` header on all payment initiation endpoints:
1. `payment-service` checks if the key already exists in PostgreSQL (`payments.idempotency_key` with unique index).
2. **Exact Replay**: If the key exists with identical transfer parameters (sender, receiver, amount), the system skips processing and returns HTTP 200 OK with the existing payment record.
3. **Payload Mismatch**: If the key exists but transfer parameters differ, the request is immediately rejected with HTTP 409 Conflict (`IDEMPOTENCY_PAYLOAD_MISMATCH`).
4. **New Transfer**: If the key is new, the payment is created in `INITIATED` status and returns HTTP 202 Accepted.

## Consequences
- **Positive**: Strict protection against double-billing, safe network retries, transparent client semantics.
- **Trade-off**: Clients must manage unique UUID generation per distinct transfer attempt.
