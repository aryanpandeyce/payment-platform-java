# Software Specification Document (SSD)
## Payment Platform (Mini Banking & Transaction System)

---

## 1. Introduction

### 1.1 Purpose
The purpose of this payment platform is to provide a reliable, event-driven banking transaction system. It handles user accounts, balances, and fund transfers between accounts with high reliability, idempotency, fraud detection, and auditability.

> **DESIGN DECISION**: Build as an event-driven microservices architecture because banking transfers require loose coupling, resilience against service downtime, and decoupled audit/notification workflows.

---

### 1.2 Scope

#### In-Scope
- **Account Management**: Creating accounts and checking account balances.
- **Payment Processing**: Initiating money transfers between accounts.
- **Idempotency Protection**: Preventing duplicate debits or credits when client network requests are retried.
- **Fraud Validation**: Rule-based transaction checks (maximum transaction amount limit and velocity check).
- **Settlement**: Safely updating sender and receiver balances with transaction consistency.
- **Notifications**: Asynchronously notifying users of payment status updates via mock console logs.
- **Audit Logging**: Persisting an append-only, immutable history of all major domain events in MongoDB.
- **Live Status & Dashboard**: A web frontend (React + Redux Toolkit) allowing users to submit payments, view transaction histories, and track payment lifecycle status in real time.
- **API Gateway & Auth**: Routing incoming requests and verifying stateless JWT tokens.

#### Out-of-Scope
- Direct integration with real-world payment rails (such as UPI, SWIFT, FedNow, Visa/Mastercard, or Stripe).
- Real third-party SMS or email delivery providers (e.g., Twilio, SendGrid).
- Multi-currency conversions and foreign exchange (FX) rates (single currency only).
- Advanced banking products (loans, credit limits, interest calculation, fixed deposits, or physical cards/ATMs).
- Full legal KYC/AML identity document verification pipelines.

> **DESIGN DECISION**: Keep external banking rails and third-party telecom APIs out-of-scope because this is an educational platform focused on mastering distributed systems patterns (Saga, Outbox, Idempotency) without third-party vendor complexity or operational costs.

---

### 1.3 Future Scope
- Multi-currency support and real-time FX rate conversion engine.
- Simulator for external clearinghouse settlement (e.g., ISO 20022 message simulator).
- Machine-learning-based anomaly detection for fraud scoring.
- Webhook subscriptions for external merchant callbacks.

---

### 1.4 Glossary

- **Idempotency**: An operation property where executing the action multiple times produces the exact same result as executing it once. In payments, if a user taps "Pay" twice or a network timeout causes a retry, the system debits the account only once.
  - *Real-life Analogy*: An elevator call button. Pressing it 10 times does not call 10 elevators; it registers the request only once.

- **Saga Pattern**: An architectural pattern that coordinates distributed transactions across multiple microservices through a sequence of local transactions and compensating actions (rollbacks) if any step fails.
  - *Real-life Analogy*: Booking a vacation package (Flight + Hotel + Cab). If hotel booking fails, the system automatically cancels the flight and cab so you are not left stranded with a half-booked trip.

- **Outbox Pattern**: A technique where database changes and their corresponding outgoing events are saved in the same local database transaction before being published to Kafka. This guarantees that events are never lost even if the message broker is temporarily down.
  - *Real-life Analogy*: Writing a letter and saving it in your office "Outbox" tray while noting it in your register. Even if the mail courier arrives late, the letter is safely recorded and ready to be delivered.

- **Aggregate**: In Domain-Driven Design (DDD), a cluster of related entity and value objects treated as a single unit for data changes, enforced by an Aggregate Root.
  - *Real-life Analogy*: A physical bank account passbook. You cannot directly scribble a balance change; all deposits or withdrawals must pass through the bank teller (Aggregate Root) who enforces the account rules.

- **Bounded Context**: A clear boundary in Domain-Driven Design where a specific domain model and its terms apply uniformly.
  - *Real-life Analogy*: The word "Customer" means "Account holder with a balance" to the Accounts desk, but "Risk rating and IP address" to the Fraud desk. Each desk operates in its own Bounded Context.

- **Event**: An immutable record of an action that has already occurred in the system (e.g., `PaymentInitiated`, `PaymentSettled`).
  - *Real-life Analogy*: A printed receipt from an ATM. Once printed, you cannot alter the fact that the transaction happened.

- **Topic**: A categorized stream in Apache Kafka where events are published and stored in order.
  - *Real-life Analogy*: A specific newspaper section (e.g., "Business" or "Sports"). Writers publish articles to it, and readers subscribe to that section.

- **Consumer Group**: A collection of Kafka consumers working together to read messages from a topic, where each partition is assigned to only one consumer in the group.
  - *Real-life Analogy*: A team of bank tellers sharing a single line of customers. Each customer is handled by only one teller so work is balanced and no customer is served twice.

- **JWT (JSON Web Token)**: A compact, cryptographically signed token containing claims about a user identity, allowing services to verify identity without querying a database on every request.
  - *Real-life Analogy*: A wristband stamped at an amusement park entrance. Ride operators just check the wristband stamp instead of calling the ticket office every time you enter a ride.

> **DESIGN DECISION**: Explicitly define domain and distributed systems terms up front because clear terminology ensures the team and codebase speak the same ubiquitous language throughout development.

---

## 2. Functional Requirements

### 2.1 Core Monetary & Invariant Rules
Every service and operation handling money in this platform must adhere to the following non-negotiable financial invariants:
1. **Precision Rule**: All monetary values must use `BigDecimal` with a fixed scale of 2 decimal places and `RoundingMode.HALF_EVEN` (banker's rounding). Floating point numbers (`float`, `double`) are strictly forbidden.
2. **Positive Amount Rule**: All transaction and transfer amounts must be strictly greater than zero (`amount > 0.00`). Zero or negative transfers are rejected immediately.
3. **Distinct Parties Rule**: The sender account and receiver account must be different (`senderAccountId != receiverAccountId`). Self-transfers are rejected.
4. **Non-Negative Balance Rule**: An account balance can never drop below zero (`balance >= 0.00`). No overdraft is permitted.

> **DESIGN DECISION**: Use `BigDecimal` instead of `double`/`float` because floating-point arithmetic causes IEEE-754 binary precision inaccuracies (e.g. `0.1 + 0.2 = 0.30000000000000004`), which lead to compounding financial discrepancies in banking ledgers.

> **DESIGN DECISION**: Disallow sender == receiver and enforce amount > 0 at both API Gateway and domain entity levels because zero or self-transfers pollute ledger data and waste system resources without moving actual value.

---

### 2.2 Account Service

*Why this matters in a real payment company*: A bank cannot operate without ledger accounts and strict balance controls that guarantee customer deposits are accurately tracked and never overdrawn.

#### FR-1: Create Account
- **Specification**: The system shall allow creating a new customer account with an initial non-negative balance and a designated currency (default: `INR`).
- **Acceptance Criteria**:
  - **Given** a valid customer ID, currency `INR`, and initial balance >= 0,
  - **When** a `POST /api/accounts` request is received,
  - **Then** a new account is persisted with a unique UUID `accountId`, status `ACTIVE`, specified initial balance, and HTTP 201 Created is returned with the account details.
  - **Given** an initial balance < 0,
  - **When** a `POST /api/accounts` request is received,
  - **Then** the request is rejected with HTTP 400 Bad Request and error code `INVALID_INITIAL_BALANCE`.

#### FR-2: Check Account Balance
- **Specification**: The system shall retrieve the current balance and status of an account by its unique account ID.
- **Acceptance Criteria**:
  - **Given** an existing active account ID,
  - **When** a `GET /api/accounts/{accountId}` request is received,
  - **Then** the system returns HTTP 200 OK containing `accountId`, `status`, `currency`, and `currentBalance`.
  - **Given** an account ID that does not exist,
  - **When** a `GET /api/accounts/{accountId}` request is received,
  - **Then** the system returns HTTP 404 Not Found with error code `ACCOUNT_NOT_FOUND`.

#### FR-3: Reserve / Debit Funds
- **Specification**: The system shall debit funds from an account only if the account is `ACTIVE` and has sufficient available balance (`balance >= amount`).
- **Acceptance Criteria**:
  - **Given** an account with status `ACTIVE` and balance 1,000.00,
  - **When** a debit of 400.00 is requested,
  - **Then** the balance is atomically decremented to 600.00 and success is returned.
  - **Given** an account with balance 1,000.00,
  - **When** a debit of 1,500.00 is requested,
  - **Then** the debit is rejected with error code `INSUFFICIENT_FUNDS` and the balance remains 1,000.00.

#### FR-4: Credit Funds
- **Specification**: The system shall credit funds to a receiver's account only if the account is `ACTIVE`.
- **Acceptance Criteria**:
  - **Given** an active account with balance 500.00,
  - **When** a credit of 400.00 is requested,
  - **Then** the balance is atomically incremented to 900.00 and success is returned.

---

### 2.3 Payment Service

*Why this matters in a real payment company*: The payment service is the central transaction coordinator. It ensures transfers are created once, tracked accurately through their lifecycle, and protected against duplicate charges.

#### FR-5: Initiate Payment with Idempotency Key
- **Specification**: The system shall accept payment requests containing an `Idempotency-Key` header, validate request parameters, persist the payment in `INITIATED` status, and return the same existing record without secondary execution if the same key is reused.
- **Acceptance Criteria**:
  - **Given** a valid transfer request with a new `Idempotency-Key: "key-123"`, valid sender ID, receiver ID (`sender != receiver`), and amount > 0,
  - **When** `POST /api/payments` is received,
  - **Then** a new payment record is saved with status `INITIATED`, linked to `key-123`, and HTTP 202 Accepted is returned with payment ID.
  - **Given** an existing payment previously initiated with `Idempotency-Key: "key-123"` and identical payload,
  - **When** `POST /api/payments` is received again with `Idempotency-Key: "key-123"`,
  - **Then** the system creates no new payment, initiates no second debit, and returns HTTP 200 OK with the existing payment record and current status.
  - **Given** an existing payment previously initiated with `Idempotency-Key: "key-123"` for amount 500.00,
  - **When** `POST /api/payments` is received with `Idempotency-Key: "key-123"` but amount 1,000.00 (payload mismatch),
  - **Then** the system rejects the request with HTTP 409 Conflict (or HTTP 400) and error code `IDEMPOTENCY_PAYLOAD_MISMATCH`.

#### FR-6: Payment Lifecycle & State Machine Transitions
- **Specification**: The system shall enforce a strict state machine:
  - Allowed forward transitions:
    - `INITIATED` -> `VALIDATED` (when fraud checks pass)
    - `INITIATED` -> `FAILED` (when fraud check fails or debit fails)
    - `VALIDATED` -> `SETTLED` (when both debit and credit complete successfully)
    - `VALIDATED` -> `FAILED` (when settlement balance deduction fails or compensation triggers)
  - Disallowed transitions: Any transition from terminal states (`SETTLED`, `FAILED`) to any other state is strictly prohibited (e.g., `SETTLED` -> `INITIATED` or `FAILED` -> `SETTLED`).
  - Only the **Payment Service** is authorized to update payment status.
- **Acceptance Criteria**:
  - **Given** a payment in `SETTLED` status,
  - **When** an event or service attempts to update its status to `INITIATED` or `FAILED`,
  - **Then** the transition is rejected with `INVALID_STATE_TRANSITION` and the status remains `SETTLED`.

#### FR-7: Payment Failure Recording
- **Specification**: When a payment fails at any point in the workflow, the system shall record the exact failure reason and transition to `FAILED`.
- **Acceptance Criteria**:
  - **Given** a payment in `INITIATED` or `VALIDATED` status,
  - **When** a failure occurs (e.g., fraud rejection or insufficient funds),
  - **Then** the payment status is updated to `FAILED`, the `failureReason` field is populated with the specific error code, and a `PaymentFailed` event is published.

> **DESIGN DECISION**: Enforce strict state machine transitions where only Payment Service can mutate payment status because decentralized status mutations lead to race conditions, partial updates, and inconsistent ledger states across microservices.

---

### 2.4 Fraud Service

*Why this matters in a real payment company*: Fraud checks act as an automated security shield, stopping stolen credentials, rogue automated bots, and high-risk drain attempts before money leaves the bank.

#### FR-8: Maximum Amount Limit Check
- **Specification**: The system shall evaluate payment amounts against a configurable maximum single transfer limit (Default: ₹50,000.00).
- **Acceptance Criteria**:
  - **Given** the configured limit is ₹50,000.00,
  - **When** a payment of ₹50,000.00 is evaluated,
  - **Then** the fraud rule passes.
  - **Given** the configured limit is ₹50,000.00,
  - **When** a payment of ₹50,001.00 is evaluated,
  - **Then** the fraud check fails with reason `EXCEEDS_MAX_AMOUNT_LIMIT`.

#### FR-9: Transfer Velocity Check
- **Specification**: The system shall evaluate the number of transfers attempted by a sender account within a sliding 1-minute window against a configurable limit (Default: 5 transfers per minute).
- **Acceptance Criteria**:
  - **Given** a sender account has made 4 transfers in the previous 60 seconds,
  - **When** a 5th transfer is evaluated,
  - **Then** the velocity check passes.
  - **Given** a sender account has made 5 transfers in the previous 60 seconds,
  - **When** a 6th transfer is evaluated within the same window,
  - **Then** the fraud check fails with reason `VELOCITY_LIMIT_EXCEEDED`.

> **DESIGN DECISION**: Make fraud limits configurable via external configuration properties (`application.yml` or environment variables) rather than hardcoded constants because risk thresholds change dynamically according to compliance policies without requiring source code rebuilds.

---

### 2.5 Settlement

*Why this matters in a real payment company*: Settlement is the moment of truth where money actually moves from sender to receiver. It must guarantee that money is neither created from thin air nor destroyed.

#### FR-10: Coordinated Settlement Execution
- **Specification**: The system shall execute settlement only after a payment has successfully reached `VALIDATED` status. It shall debit the sender account and credit the receiver account.
- **Acceptance Criteria**:
  - **Given** a payment with status `VALIDATED`,
  - **When** settlement executes,
  - **Then** the sender account is debited by the payment amount, the receiver account is credited by the payment amount, the payment transitions to `SETTLED`, and a `PaymentSettled` event is published.

#### FR-11: Settlement Failure & Compensation
- **Specification**: If sender debit succeeds but receiver credit cannot complete (e.g. receiver account blocked or suspended), the system shall trigger a compensation action to refund the sender account.
- **Acceptance Criteria**:
  - **Given** sender account was debited 500.00,
  - **When** receiver credit fails,
  - **Then** a compensating credit of 500.00 is executed on the sender account, the payment transitions to `FAILED` with reason `RECEIVER_CREDIT_FAILED_REFUNDED`, and a `PaymentFailed` event is published.

> **DESIGN DECISION**: Implement atomic debit/credit coordination with compensating reversal because distributed two-phase commit (2PC) locks resources and degrades availability across autonomous microservices.

---

### 2.6 Notification Service

*Why this matters in a real payment company*: Customers expect instant alerts on financial activity to confirm their transaction succeeded or immediately notice unauthorized account activity.

#### FR-12: Asynchronous Event-Driven Notifications
- **Specification**: The system shall consume payment status events (`PaymentSettled`, `PaymentFailed`) from Kafka and output mock notification messages to console logs (simulating Email/SMS delivery).
- **Acceptance Criteria**:
  - **Given** a `PaymentSettled` event on Kafka topic `payment-events`,
  - **When** consumed by the Notification Service,
  - **Then** a log is output: `[NOTIFICATION] [SMS/EMAIL] Transfer of {amount} from Account {senderId} to Account {receiverId} completed successfully. Payment ID: {paymentId}`.
  - **Given** a `PaymentFailed` event on Kafka topic `payment-events`,
  - **When** consumed by the Notification Service,
  - **Then** a log is output: `[NOTIFICATION] [SMS/EMAIL] Transfer of {amount} failed. Reason: {failureReason}. Payment ID: {paymentId}`.

> **DESIGN DECISION**: Run notification delivery asynchronously via Kafka consumer rather than synchronously in the payment HTTP request path because slow or failing notification systems should never block or delay money settlement.

---

### 2.7 Audit Logging Service

*Why this matters in a real payment company*: Financial regulators (e.g., RBI, SEC, Federal Reserve) require a permanent, unalterable ledger trail of every single state change and financial event for legal accountability.

#### FR-13: Centralized Immutable Audit Trail
- **Specification**: The system shall consume all domain events from Kafka and persist them as immutable, append-only documents in MongoDB.
- **Acceptance Criteria**:
  - **Given** any domain event published on Kafka (`AccountCreated`, `PaymentInitiated`, `PaymentValidated`, `PaymentSettled`, `PaymentFailed`),
  - **When** consumed by the Audit Service,
  - **Then** an audit document is inserted into MongoDB with `eventId`, `eventType`, `aggregateId`, `timestamp`, `payload`, and `correlationId`.
  - **Given** an existing audit document in MongoDB,
  - **When** an update or delete operation is attempted,
  - **Then** the operation is rejected (append-only enforcement; no update/delete APIs exposed).

> **DESIGN DECISION**: Store audit logs in MongoDB as append-only documents because document databases naturally store arbitrary JSON event payloads without rigid schema migrations, and immutable append-only storage satisfies regulatory compliance.

---

### 2.8 API Gateway & Authentication

*Why this matters in a real payment company*: The API Gateway is the bank's frontline security guard. It inspects all incoming internet traffic, validates security badges (JWTs), and shields internal microservices from direct public exposure.

#### FR-14: Stateless JWT Authentication & Request Routing
- **Specification**: The API Gateway shall validate incoming Bearer JWT tokens on protected endpoints (`/api/accounts/**`, `/api/payments/**`), extract verified identity claims, forward the request with an `X-User-Id` header, and route traffic to the appropriate microservice.
- **Acceptance Criteria**:
  - **Given** an HTTP request with a valid, signed JWT in the `Authorization: Bearer <token>` header,
  - **When** received at the API Gateway,
  - **Then** the gateway verifies token signature and expiration, extracts user ID, injects header `X-User-Id`, and routes request to the target microservice.
  - **Given** an HTTP request with a missing, expired, or tampered JWT,
  - **When** received at the API Gateway,
  - **Then** the gateway rejects the request with HTTP 401 Unauthorized without forwarding traffic downstream.

> **DESIGN DECISION**: Terminate and verify JWTs at the API Gateway so individual downstream microservices do not need to duplicate cryptographic token validation or manage user authentication logic.

---

### 2.9 Frontend Dashboard

*Why this matters in a real payment company*: A bank's customers and operations staff need an intuitive, transparent interface to initiate transfers, review balances, and monitor live status without confusion.

#### FR-15: Payment Initiation Form & Double-Click Prevention
- **Specification**: The frontend shall provide a payment submission form that enforces client-side validation (amount > 0, sender != receiver), generates a unique UUID `Idempotency-Key` per transfer attempt, and disables the submit button during submission.
- **Acceptance Criteria**:
  - **Given** the payment transfer form,
  - **When** the user clicks "Send Payment",
  - **Then** a unique UUID v4 `Idempotency-Key` is generated, the submit button is disabled with a loading spinner, and the `POST /api/payments` request is sent with the header.

#### FR-16: Live Status & Transaction History Display
- **Specification**: The frontend shall display account details, current balance, recent transaction history, and real-time status badges (`INITIATED`, `VALIDATED`, `SETTLED`, `FAILED`).
- **Acceptance Criteria**:
  - **Given** a user viewing the transaction dashboard,
  - **When** a payment lifecycle event is completed in the backend,
  - **Then** the UI reflects the updated payment status badge and refreshed account balances.

---

### 2.10 Out of Scope for Section 2
- Scheduled or recurring payments (standing instructions).
- Partial refunds and dispute/chargeback workflows.
- International wire transfers and currency conversion.
- Overdraft protection and credit lending limits.
- Bulk batch payroll processing.

### 2.11 Future Scope
- Multi-factor authentication (SMS/TOTP 2FA) before authorizing high-value transfers.
- Dynamic machine-learning fraud scoring engine based on device fingerprinting and geolocation.
- Split-payment capability (dividing a single bill across multiple funding accounts).

