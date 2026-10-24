# ADR-011: Observability Architecture — ELK vs. Loki and Correlation ID Tracing

## Status
Accepted

## Context
A mission-critical distributed payment banking architecture requires comprehensive observability:
1. **Metrics**: Real-time throughput, latency, failure rates, and business KPIs.
2. **Logs**: Centralized log aggregation, fast full-text searching, filtering by tenant/service, and structured log indexing.
3. **Tracing**: Tracking requests across gateway, synchronous REST calls, asynchronous Kafka topics, and background outbox workers.
4. **Local Hardware Constraints**: Developers run the full stack locally on personal workstations (Windows, Linux, macOS) alongside Docker Desktop. Running heavyweight observability platforms can cause RAM exhaustion.

Two major architectures were evaluated:
- **Option A: Elastic Stack (Elasticsearch + Logstash / Fluent Bit + Kibana)**: Industry-standard enterprise log aggregation and analysis.
- **Option B: Grafana Loki + Tempo + Prometheus**: Lightweight log indexing without full-text inverted indexes.

## Decision
1. **Metrics with Prometheus & Grafana**:
   - Actuator endpoints scrape Micrometer metrics (`/actuator/prometheus`).
   - Custom business metrics: `payments_initiated_total`, `payments_settled_total`, `payments_failed_total{reason}`, `payment_settlement_duration_seconds`, `fraud_rejections_total{rule}`, `outbox_pending_events`, `kafka_dlt_messages_total`, and `auth_login_failures_total`.
   - Grafana provisioned as code with datasources, Prometheus alert rules, and three pre-configured dashboards (Platform Overview, Payments Business, Kafka Consumers).

2. **Log Aggregation with ELK (Elasticsearch + Fluent Bit + Kibana)**:
   - Microservices use `logstash-logback-encoder` to output structured JSON logs with MDC context.
   - Fluent Bit acts as the lightweight log shipper forwarding logs directly to Elasticsearch 8.17 single-node (security disabled for local dev; TLS + RBAC in production).
   - Pre-configured Kibana data view (`apexpay-*`) and saved search for correlation ID tracing.
   - **Trade-off vs. Loki**: Elasticsearch was selected because banking compliance teams and global payment service operations require sub-second inverted-index full-text search across historical transaction logs and exact error message matches, which Loki's grep-like streaming model cannot deliver as efficiently at scale.

3. **Tracing Strategy: Explicit `X-Correlation-Id` vs. OpenTelemetry**:
   - Instead of heavyweight OpenTelemetry byte-code manipulation or Brave agents that add 200MB+ memory overhead per container and introduce reactive WebFlux vs. servlet thread-boundary friction, we implemented a lightweight, explicit Correlation ID pattern (`X-Correlation-Id`).
   - The API Gateway generates or propagates the correlation ID into downstream HTTP request headers and MDC.
   - Kafka producers add the correlation ID as a byte header in message records, and consumers restore it into MDC before processing.
   - Structured JSON logs output the correlation ID on every single log event.

4. **Data Protection & PII Masking Guardrails**:
   - In accordance with banking privacy standards (PCI-DSS), passwords, bearer tokens, and full account numbers are masked before reaching log storage via `DataMaskingUtil` and log pattern filters.

## Consequences & Trade-offs
- **Positive**:
  - Full-text search and filtering in Kibana with zero license cost.
  - End-to-end auditability from UI request to database write and Kafka consumer using a single Correlation ID.
  - Zero sensitive credentials leaked in log storage.
- **Negative / Trade-offs**:
  - Elasticsearch single-node container requires ~512MB-1GB heap; separated into `docker-compose.observability.yml` so it remains optional for minimal local test runs.
