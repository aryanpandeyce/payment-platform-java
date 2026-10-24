# Contributing to Payment Platform (Java)

Thank you for your interest in contributing! This project is an enterprise-grade payment and banking transaction microservices system built with Spring Boot 3, Apache Kafka, PostgreSQL, MongoDB, and React.

## Development Principles & Rules

1. **Test-Driven Development (TDD)**:
   - Always write tests first.
   - Run `.\mvnw.cmd test -B` to ensure existing and new unit & integration tests pass.
2. **Conventional Commits**:
   - Format commit messages as:
     - `feat(<service>): add new feature`
     - `fix(<service>): resolve bug`
     - `refactor(<service>): code restructuring`
     - `docs: update documentation or specs`
     - `test(<service>): add unit/integration tests`
3. **Financial Invariants (Non-Negotiable)**:
   - Always use `BigDecimal` with scale 2 and `RoundingMode.HALF_EVEN`. Never use `double` or `float` for monetary amounts.
   - All amounts must be strictly positive (`> 0.00`).
   - Self-transfers (`senderId == receiverId`) are strictly prohibited.
   - Account balances cannot be negative (no overdraft).
4. **Idempotency & Resilience**:
   - All payment mutation endpoints must accept and enforce an `Idempotency-Key` header.
   - Database mutations accompanied by event emissions must use the **Transactional Outbox Pattern**.
   - Kafka message handlers must be idempotent and handle duplicate event delivery safely.
5. **Clean Architecture**:
   - Package structure follows DDD: `api` (controllers & DTO records), `application` (orchestration services), `domain` (aggregates, entities, domain exceptions), `infrastructure` (repositories, Kafka consumers, filters, configs).
   - Never expose JPA entities directly in HTTP responses; always use immutable Java records for DTOs.
6. **No Secrets in Source Code**:
   - Never commit passwords, tokens, or private keys. Externalize configuration via `.env` and environment variables.

## Getting Started

1. Fork and clone the repository.
2. Ensure you have Java 21, Maven, Docker, and Node.js 20+ installed.
3. Start the infrastructure:
   ```powershell
   docker compose up -d postgres kafka mongodb
   ```
4. Run all microservice tests:
   ```powershell
   .\mvnw.cmd test -B
   ```
5. Submit a pull request targeting the `main` branch.
