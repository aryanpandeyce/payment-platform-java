# ADR-006: Maven Multi-Module Parent POM Architecture

## Status
Accepted

## Context
In Phase 1, the repository was organized as a collection of independent Maven projects (`account-service`, `payment-service`, `fraud-service`, `notification-service`, `api-gateway`). Each service had its own `pom.xml` inheriting directly from `spring-boot-starter-parent`. While this allowed rapid prototyping, it introduced several maintenance and governance liabilities:
1. Fragmented dependency versions across services (risk of incompatible transitive libraries).
2. Repetitive boilerplate configuration (compiler flags, plugin settings, test configurations).
3. No single command to build, verify, and run tests across all microservices from the root directory.
4. Duplicate code across services for standard DTOs (e.g., standard `ErrorResponse`), event payloads, and correlation ID utilities.
5. Inability to enforce uniform governance policies (e.g. strict Java 21 compilation, SonarQube / JaCoCo aggregated reporting).

## Decision
We convert the repository into a Maven multi-module project with a root parent POM (`packaging=pom`) and the following modules:
1. `common`: Shared standard library (pure Java / Spring MVC / WebFlux compatible records: `ErrorResponse`, correlation ID utilities, event payload records).
2. `identity-service`: Dedicated identity, authentication, and token management service.
3. `account-service`: Ledger and balance management service.
4. `payment-service`: Payment transaction orchestration and outbox service.
5. `fraud-service`: Velocity and risk limit evaluation service.
6. `notification-service`: Kafka consumer and MongoDB audit log store.
7. `api-gateway`: Spring Cloud Gateway with JWT authentication, RBAC header injection, and rate limiting.

### Governance & Build Standards
- **Java 21 Enforcement**: The root POM configures `maven-enforcer-plugin` requiring Java `[21, 22)`.
- **Centralized Dependencies**: All common dependency versions (`spring-boot-dependencies`, `spring-cloud-dependencies`, `testcontainers-bom`, `micrometer-registry-prometheus`) are pinned in the root `dependencyManagement`.
- **Plugin Management**: `pluginManagement` centralizes versions and configurations for `maven-compiler-plugin`, `maven-surefire-plugin`, `maven-failsafe-plugin`, `jacoco-maven-plugin`, and `spring-boot-maven-plugin` (with `build-info` enabled).
- **Test Separation**: Surefire executes fast unit tests (`*Test.java`), while Failsafe runs integration and Testcontainers tests (`*IT.java`, `*IntegrationTest.java`, `*TestcontainersTest.java`).
- **Observability**: Every service includes Spring Boot Actuator and `micrometer-registry-prometheus`. Liveness and readiness probes (`/actuator/health/liveness`, `/actuator/health/readiness`) and metrics (`/actuator/prometheus`) are enabled internally and blocked at the Gateway.

## Consequences & Trade-offs
- **Positive**:
  - Single command verification: `.\mvnw.cmd clean verify -B` builds and tests all microservices in topological order.
  - Consistent dependency tree and zero version drift across microservices.
  - Clean separation between lightweight unit tests and heavier Testcontainers integration tests.
  - Aggregated JaCoCo reports provide a unified codebase coverage view for SonarQube and CI/CD pipelines.
- **Negative / Trade-offs**:
  - The `common` module must remain lean and strictly decoupled from database persistence or domain business logic to prevent tight coupling across microservices.
  - Rebuilding from the root takes longer than building an isolated service during local development; developers can still build a single module using `mvn -pl :account-service -am`.
