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
