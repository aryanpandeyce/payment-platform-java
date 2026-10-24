# ADR-012: In-Memory Rate Limiting vs. Distributed Redis Architecture

## Status
Accepted

## Context
Exposing authentication endpoints (`POST /auth/login`) directly to the public web introduces brute-force credential stuffing and denial-of-service risks. To mitigate this:
1. Rate limiting is required on `/auth/login` at the edge API Gateway.
2. In enterprise multi-gateway architectures, rate limiting typically relies on a shared Redis cluster (`spring-boot-starter-data-redis-reactive` with Redis Token Bucket scripts).
3. However, running a Redis cluster locally adds an additional infrastructure container (~100-200MB RAM), connection pool management, and operational complexity.
4. The goal is to provide enterprise-grade protection using strictly free, lightweight local tools without over-engineering.

## Decision
1. **Custom In-Memory Token Bucket / Sliding Window Rate Limiter**:
   - Implemented `LoginRateLimiterFilter` directly within `api-gateway`.
   - Uses a thread-safe `ConcurrentHashMap` with an in-memory sliding window deque per client IP.
   - Enforces a default threshold of 10 login requests per 60 seconds per IP.
   - Excessive requests receive HTTP `429 Too Many Requests` with a standard `ErrorResponse` and a `Retry-After: 60` HTTP header.
2. **Account-Level Lockout Complement**:
   - Complemented at the application layer in `identity-service`: 5 consecutive failed login attempts lock the user account (`status = 'LOCKED'`), requiring administrator intervention.

## Trade-offs & Production Evolution
- **Trade-off**: In-memory rate limiting state is local to each Gateway instance. If multiple Gateway replicas are deployed behind a load balancer without sticky sessions, each replica maintains its own window counter.
- **Production Migration Path**: In a multi-replica production deployment, `LoginRateLimiterFilter` can be substituted with Spring Cloud Gateway's built-in `RequestRateLimiter` backed by a managed AWS ElastiCache / Redis cluster by setting `spring.cloud.gateway.redis-rate-limiter.replenishRate` and configuring a `KeyResolver` bean.
