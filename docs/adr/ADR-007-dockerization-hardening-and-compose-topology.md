# ADR-007: Dockerization Hardening and Compose Topology

## Status
Accepted

## Context
Deploying enterprise microservices for financial workloads requires robust container security, fast startup, deterministic orchestration, and strict resource isolation:
1. **Root Execution Risk**: Running containers as `root` (UID 0) exposes the host system to container breakout vulnerabilities (CWE-250).
2. **Fat Images**: Storing build tools, source trees, and package managers inside the production runtime bloats container sizes (>500MB) and expands the CVE attack surface.
3. **Startup Race Conditions**: Services starting before their downstream dependencies (Postgres, Kafka, MongoDB) are ready cause transient connection timeouts and startup crashes.
4. **Memory Exhaustion**: The JVM running inside Docker without container support can miscalculate host memory and trigger kernel OOM kills.
5. **Stack Weight**: Bundling Prometheus, Grafana, Elasticsearch, Kibana, and SonarQube inside a single `docker-compose.yml` makes local development excessively heavy (~8GB RAM requirement).

## Decision
1. **Multi-Stage Dockerfiles**:
   - **Stage 1 (Build)**: `maven:3.9.9-eclipse-temurin-21-alpine` compiles and packages the service.
   - **Stage 2 (Extractor)**: Extracts the Spring Boot thin jar and dependency layer (`lib/`).
   - **Stage 3 (Runtime)**: `eclipse-temurin:21.0.6_7-jre-alpine` (pinned version, zero build tools).
2. **Security & Non-Root Execution**:
   - All services execute under a dedicated system user: `appuser:appgroup` (UID/GID 10001).
   - Dedicated `.dockerignore` files prevent committing credentials, `.git`, `node_modules`, or build caches into container layers.
3. **Container-Aware JVM Flags**:
   - `-XX:+UseContainerSupport`
   - `-XX:MaxRAMPercentage=75.0`
   - `-XX:InitialRAMPercentage=50.0`
   - `-XX:+ExitOnOutOfMemoryError`
   - `-Duser.timezone=Asia/Kolkata`
4. **Deterministic Healthchecks & Compose Dependencies**:
   - Every service defines an Actuator `/actuator/health/liveness` Docker `HEALTHCHECK`.
   - Databases and brokers define native health probes (`pg_isready`, `mongosh ping`, Kafka broker topic check).
   - In `docker-compose.yml`, downstream services declare `depends_on: condition: service_healthy`.
5. **Decoupled Three-Tier Compose Topologies**:
   - `docker-compose.yml`: Core application and infra (Postgres, Kafka, MongoDB, 6 microservices, React UI). Requires ~3.5GB RAM.
   - `docker-compose.observability.yml`: Optional monitoring stack (Prometheus 9090, Grafana 3000, Elasticsearch 9200, Kibana 5601, Fluent Bit). Requires ~2.5GB RAM.
   - `docker-compose.quality.yml`: Optional security & code quality stack (SonarQube 9000, Sonar Postgres). Requires ~2GB RAM.

## Consequences & Trade-offs
- **Positive**:
  - Image size reduced by over 60% compared to monolithic Maven images.
  - Zero root execution aligns with CIS Docker Benchmarks and Kubernetes / OpenShift restricted security standards.
  - Core stack runs smoothly on developer machines with 8GB-16GB RAM without forced observability overhead.
- **Negative / Trade-offs**:
  - Multi-stage builds take slightly longer on clean caches; mitigated by Docker layer caching of POMs and dependencies.
