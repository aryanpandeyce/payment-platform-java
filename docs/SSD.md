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
