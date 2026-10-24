# Build Log

Every file created in the project and the reason it exists.

## Initial Setup
- `account-service/pom.xml`: Maven build file for account service dependencies.
- `account-service/src/main/resources/application.yml`: Configuration for account service (port 8085, Postgres datasource).
- `account-service/src/main/java/com/payments/account/AccountServiceApplication.java`: Main Spring Boot entry point for account service.
- `account-service/src/main/java/com/payments/account/domain/Account.java`: Account entity aggregate root enforcing balance and transaction invariants.
- `account-service/src/main/java/com/payments/account/domain/InsufficientFundsException.java`: Domain exception thrown when debit amount exceeds available balance.
- `account-service/src/main/java/com/payments/account/domain/InvalidAmountException.java`: Domain exception thrown when amount is negative, zero or null.
- `account-service/src/test/java/com/payments/account/domain/AccountTest.java`: Unit tests verifying Account entity debit, credit and balance rules.
- `account-service/src/test/java/com/payments/account/AccountServiceApplicationTests.java`: Context load smoke test for account service.

## Phase 1: account-service (FR-1..4)
- `account-service/src/test/resources/application.yml`: In-memory H2 configuration for fast, isolated, Docker-independent testing.
- `account-service/src/main/java/com/payments/account/domain/AccountNotFoundException.java`: Domain exception thrown when an account ID is not found in database.
- `account-service/src/main/java/com/payments/account/repository/AccountRepository.java`: Spring Data JPA repository for Account entity database operations.
- `account-service/src/main/java/com/payments/account/dto/CreateAccountRequest.java`: DTO validating request body for POST /api/accounts.
- `account-service/src/main/java/com/payments/account/dto/AccountResponse.java`: DTO representing account metadata, balance, currency and status.
- `account-service/src/main/java/com/payments/account/dto/BalanceResponse.java`: DTO representing account balance check response.
- `account-service/src/main/java/com/payments/account/dto/TransferAmountRequest.java`: DTO validating monetary amount for debit and credit endpoints.
- `account-service/src/main/java/com/payments/account/dto/ErrorResponse.java`: Standardized JSON error response with error code, message and timestamp.
- `account-service/src/main/java/com/payments/account/service/AccountService.java`: Service coordinating transactional account creation, balance retrieval, debits, credits, and atomic transfers.
- `account-service/src/main/java/com/payments/account/controller/GlobalExceptionHandler.java`: Centralized exception handler mapping domain errors to standard HTTP status codes (400, 404, 409).
- `account-service/src/main/java/com/payments/account/controller/AccountController.java`: REST controller exposing endpoints for account management, balance, debit, and credit.
- `account-service/src/test/java/com/payments/account/controller/AccountControllerIntegrationTest.java`: Integration tests verifying FR-1 through FR-4 acceptance criteria.

## Phase 2: payment-service (FR-5..7)
- `payment-service/pom.xml`: Maven build file for payment service with Web, JPA, Kafka and H2.
- `payment-service/src/main/resources/application.yml`: Configuration for payment service on port 8086 with Postgres and Kafka.
- `payment-service/src/test/resources/application.yml`: Test configuration using in-memory H2.
- `payment-service/src/main/java/com/payments/payment/PaymentServiceApplication.java`: Main Spring Boot application entry point with @EnableScheduling.
- `payment-service/src/main/java/com/payments/payment/domain/PaymentStatus.java`: State machine status enum (INITIATED, VALIDATED, SETTLED, FAILED).
- `payment-service/src/main/java/com/payments/payment/domain/InvalidStateTransitionException.java`: Domain exception enforcing legal state transitions and rejecting mutations from terminal states.
- `payment-service/src/main/java/com/payments/payment/domain/IdempotencyPayloadMismatchException.java`: Exception thrown when the same idempotency key is submitted with a different body (HTTP 409).
- `payment-service/src/main/java/com/payments/payment/domain/PaymentNotFoundException.java`: Exception thrown when a payment ID does not exist in database (HTTP 404).
- `payment-service/src/main/java/com/payments/payment/domain/Payment.java`: Aggregate root enforcing idempotency key uniqueness, distinct parties, positive amounts, and state machine transitions.
- `payment-service/src/main/java/com/payments/payment/domain/OutboxEvent.java`: Transactional outbox table entity ensuring reliable message delivery.
- `payment-service/src/main/java/com/payments/payment/repository/PaymentRepository.java`: Spring Data JPA repository for payment entities with idempotency key lookup.
- `payment-service/src/main/java/com/payments/payment/repository/OutboxRepository.java`: Repository for querying pending outbox messages in chronological order.
- `payment-service/src/main/java/com/payments/payment/dto/InitiatePaymentRequest.java`: DTO validating incoming payment creation request.
- `payment-service/src/main/java/com/payments/payment/dto/PaymentResponse.java`: DTO returning complete payment lifecycle and metadata state.
- `payment-service/src/main/java/com/payments/payment/dto/ErrorResponse.java`: Standardized error JSON response model.
- `payment-service/src/main/java/com/payments/payment/event/PaymentInitiatedEvent.java`: Domain event published to Kafka topic `payment-initiated`.
- `payment-service/src/main/java/com/payments/payment/service/PaymentService.java`: Service coordinating idempotent creation, outbox event writing, and state transitions.
- `payment-service/src/main/java/com/payments/payment/service/OutboxPoller.java`: Background scheduled worker publishing pending outbox events to Kafka.
- `payment-service/src/main/java/com/payments/payment/controller/GlobalExceptionHandler.java`: Centralized exception handler mapping idempotency conflicts and state errors to standard HTTP status codes.
- `payment-service/src/main/java/com/payments/payment/controller/PaymentController.java`: REST controller exposing POST /api/payments, GET /api/payments/{id}, and GET /api/payments.
- `payment-service/src/test/java/com/payments/payment/domain/PaymentTest.java`: Unit tests verifying state machine transitions, terminal state enforcement, and financial invariants.
- `payment-service/src/test/java/com/payments/payment/PaymentServiceApplicationTests.java`: Context loading smoke test for payment service.
- `payment-service/src/test/java/com/payments/payment/controller/PaymentControllerIntegrationTest.java`: Integration tests verifying FR-5, FR-6, idempotency reuse, mismatch conflict, and outbox persistence.

## Phase 3: the event flow (FR-8..11)
- `fraud-service/pom.xml`: Maven build file for fraud service with Kafka integration.
- `fraud-service/src/main/resources/application.yml`: Configuration for fraud service on port 8087 with Kafka and configurable limits.
- `fraud-service/src/test/resources/application.yml`: Test configuration for fraud service.
- `fraud-service/src/main/java/com/payments/fraud/FraudServiceApplication.java`: Main entry point for fraud service.
- `fraud-service/src/main/java/com/payments/fraud/config/FraudProperties.java`: Externalized configuration for max transfer amount (₹50,000) and velocity window (5 transfers/min).
- `fraud-service/src/main/java/com/payments/fraud/service/FraudCheckResult.java`: Outcome model carrying validation pass/fail status and rejection reason.
- `fraud-service/src/main/java/com/payments/fraud/service/FraudCheckService.java`: Rule engine performing amount threshold and sliding 1-minute velocity checks.
- `fraud-service/src/main/java/com/payments/fraud/event/PaymentInitiatedEvent.java`: DTO deserializing payment-initiated events.
- `fraud-service/src/main/java/com/payments/fraud/event/PaymentValidatedEvent.java`: DTO publishing passed fraud checks to `payment-validated`.
- `fraud-service/src/main/java/com/payments/fraud/event/PaymentFailedEvent.java`: DTO publishing failed fraud checks to `payment-failed`.
- `fraud-service/src/main/java/com/payments/fraud/listener/FraudEventListener.java`: Idempotent Kafka listener consuming `payment-initiated` and routing to validated/failed topics.
- `fraud-service/src/test/java/com/payments/fraud/service/FraudCheckServiceTest.java`: Unit tests verifying FR-8 (amount limit) and FR-9 (velocity limit) acceptance criteria.
- `fraud-service/src/test/java/com/payments/fraud/FraudServiceApplicationTests.java`: Context loading smoke test for fraud service.
- `account-service/src/main/java/com/payments/account/event/PaymentValidatedEvent.java`: Event model for incoming settlement requests.
- `account-service/src/main/java/com/payments/account/event/PaymentSettledEvent.java`: Event model publishing successful atomic balance settlement.
- `account-service/src/main/java/com/payments/account/event/PaymentFailedEvent.java`: Event model publishing settlement failure reasons (e.g. insufficient funds, missing account).
- `account-service/src/main/java/com/payments/account/listener/SettlementEventListener.java`: Idempotent consumer executing atomic sender debit and receiver credit with compensation handling.
- `payment-service/src/main/java/com/payments/payment/event/PaymentValidatedEvent.java`: Event model for validated status updates.
- `payment-service/src/main/java/com/payments/payment/event/PaymentSettledEvent.java`: Event model for settled status updates.
- `payment-service/src/main/java/com/payments/payment/event/PaymentFailedEvent.java`: Event model for failed status updates.
- `payment-service/src/main/java/com/payments/payment/listener/PaymentStatusEventListener.java`: Idempotent consumer advancing payment state machine to VALIDATED, SETTLED, or FAILED.

## Phase 4: notification-service + audit (FR-12, 13)
- `notification-service/pom.xml`: Maven build file for notification and MongoDB audit logging service.
- `notification-service/src/main/resources/application.yml`: Configuration for notification service on port 8088 with Kafka and MongoDB.
- `notification-service/src/test/resources/application.yml`: Test configuration for notification service.
- `notification-service/src/main/java/com/payments/notification/NotificationServiceApplication.java`: Main Spring Boot application entry point for notifications and audit logging.
- `notification-service/src/main/java/com/payments/notification/domain/AuditLog.java`: MongoDB document entity for immutable append-only event trail.
- `notification-service/src/main/java/com/payments/notification/repository/AuditLogRepository.java`: Spring Data MongoDB repository for persisting and querying audit log entries.
- `notification-service/src/main/java/com/payments/notification/listener/NotificationEventListener.java`: Kafka consumer generating mock SMS/Email console logs for settled and failed payments.
- `notification-service/src/main/java/com/payments/notification/listener/AuditEventListener.java`: Kafka consumer capturing all domain events and persisting them into MongoDB.
- `notification-service/src/test/java/com/payments/notification/NotificationServiceApplicationTests.java`: Context loading smoke test for notification service.
- `notification-service/src/test/java/com/payments/notification/listener/AuditEventListenerTest.java`: Unit tests verifying event deserialization and MongoDB audit document persistence.
