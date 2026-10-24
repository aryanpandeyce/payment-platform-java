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

## Phase 5: api-gateway + JWT (FR-14)
- `api-gateway/pom.xml`: Maven build file for Spring Cloud Gateway with reactive Netty and JJWT.
- `api-gateway/src/main/resources/application.yml`: Configuration routing `/api/accounts/**` and `/api/payments/**` on port 8090.
- `api-gateway/src/test/resources/application.yml`: Test configuration for api-gateway.
- `api-gateway/src/main/java/com/payments/gateway/ApiGatewayApplication.java`: Main entry point for Spring Cloud Gateway.
- `api-gateway/src/main/java/com/payments/gateway/security/JwtUtil.java`: Utility generating HMAC-SHA256 signed JWT tokens and validating claims.
- `api-gateway/src/main/java/com/payments/gateway/security/JwtAuthenticationFilter.java`: GlobalFilter enforcing Bearer token authentication and injecting `X-User-Id`.
- `api-gateway/src/main/java/com/payments/gateway/dto/LoginRequest.java`: DTO for POST /auth/login credentials.
- `api-gateway/src/main/java/com/payments/gateway/dto/LoginResponse.java`: DTO returning JWT token and metadata.
- `api-gateway/src/main/java/com/payments/gateway/controller/AuthController.java`: Reactive REST controller authenticating demo credentials and issuing JWTs.
- `api-gateway/src/test/java/com/payments/gateway/ApiGatewayApplicationTests.java`: Context loading smoke test for api-gateway.
- `api-gateway/src/test/java/com/payments/gateway/security/JwtUtilTest.java`: Unit tests verifying JWT signing, claim extraction, and tamper detection.
- `api-gateway/src/test/java/com/payments/gateway/controller/AuthControllerTest.java`: WebTestClient integration test verifying POST /auth/login with valid and invalid credentials.

## Phase 6: frontend (FR-15, 16)
- `frontend/package.json`: Vite, React 18, Redux Toolkit, and Lucide React icons manifest.
- `frontend/vite.config.js`: Vite build configuration with relative base for GitHub Pages and proxy to Gateway.
- `frontend/index.html`: Main HTML entry point with Plus Jakarta Sans and JetBrains Mono fonts.
- `frontend/.gitignore`: Ignores node_modules and build artifacts in frontend directory.
- `frontend/src/index.css`: Curated dark-mode fintech design system with glassmorphic cards and glowing badges.
- `frontend/src/main.jsx`: React entry point wrapping the root component in Redux Provider.
- `frontend/src/App.jsx`: Main dashboard coordinating 2-second live polling, authentication state, and modals.
- `frontend/src/services/api.js`: API client supporting both live Gateway endpoints and full offline mock event choreography.
- `frontend/src/store/index.js`: Centralized Redux Toolkit store.
- `frontend/src/store/authSlice.js`: Redux slice managing JWT token, demo credentials, and mock mode toggle.
- `frontend/src/store/accountsSlice.js`: Redux slice tracking account ledger balances and active statuses.
- `frontend/src/store/paymentsSlice.js`: Redux slice managing payment submission flight state and real-time transaction history.
- `frontend/src/components/Navbar.jsx`: Header featuring system branding, mock mode indicator, and logout.
- `frontend/src/components/LoginView.jsx`: Glassmorphic authentication card with 1-click demo login.
- `frontend/src/components/AccountCards.jsx`: Visual grid of bank accounts with balances, copy ID button, and creation modal.
- `frontend/src/components/PaymentForm.jsx`: Double-click protected form with unique Idempotency-Key preview and quick amount chips.
- `frontend/src/components/TransactionList.jsx`: Live auto-refreshing transaction table with pulsing status badges and failure reasons.
- `frontend/src/components/ArchitectureModal.jsx`: Interactive slideover explaining the 7-step event-driven architecture.

## Phase 7: ship it
- `account-service/Dockerfile`: Multi-stage Eclipse Temurin Java 21 Alpine container build for account service.
- `payment-service/Dockerfile`: Multi-stage Eclipse Temurin Java 21 Alpine container build for payment service.
- `fraud-service/Dockerfile`: Multi-stage Eclipse Temurin Java 21 Alpine container build for fraud service.
- `notification-service/Dockerfile`: Multi-stage Eclipse Temurin Java 21 Alpine container build for notification service.
- `api-gateway/Dockerfile`: Multi-stage Eclipse Temurin Java 21 Alpine container build for API gateway.
- `frontend/Dockerfile`: Multi-stage Node 22 build and Nginx Alpine container serving production React dashboard.
- `docker/init-dbs.sh`: PostgreSQL entrypoint initialization script creating `account_db` and `payment_db`.
- `docker-compose.yml`: Comprehensive multi-container setup with dual-listener Kafka (KRaft), PostgreSQL, MongoDB, all 5 Java microservices, and React dashboard.
- `.github/workflows/ci.yml`: GitHub Actions CI/CD workflow running Maven tests across all microservices and deploying the React frontend to GitHub Pages.
- `docs/DEMO.md`: 2-minute step-by-step PowerShell demo script verifying authentication, account creation, transfer settlement, idempotency, fraud breach, and MongoDB audits.
- `README.md`: Comprehensive system documentation with Mermaid architecture diagram, port registry, patterns, quickstart instructions, and known limitations.

