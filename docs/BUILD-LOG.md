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
