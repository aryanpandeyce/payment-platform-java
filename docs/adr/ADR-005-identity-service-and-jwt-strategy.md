# ADR-005: Dedicated Identity Service, Dual-Token Strategy and Gateway RBAC

## Status
Accepted

## Context
In Phase 1, authentication was mocked at the API Gateway using a hardcoded `admin` / `password` credential check. For Phase 2 production readiness and DevSecOps compliance, the platform requires:
1. Real user identity management (UUIDs, usernames, emails, full names, password hashing, account lockout).
2. Role-Based Access Control (RBAC) separating administrative actions from standard user accounts.
3. Resilient session management that minimizes token theft risk without requiring heavy database queries on every downstream API call.

## Decision
1. **Dedicated `identity-service`**: Maintain bounded contexts by giving Identity its own microservice and PostgreSQL database (`identity_db`). Identity manages user registrations, BCrypt password hashing (cost factor 12), failed attempt counters, and single-use refresh token rotation.
2. **Dual-Token Lifecycle**:
   - **Access Token**: Short-lived (15 minutes), signed via HMAC-SHA256 (`JWT_SECRET`), containing claims: `sub` (userId UUID), `username`, and `role`.
   - **Refresh Token**: Long-lived (7 days), cryptographically secure random string stored hashed in PostgreSQL. On each refresh (`POST /auth/refresh`), the old token is revoked and a new pair is issued (token rotation).
3. **Gateway Enforcement & Header Propagation**:
   - The API Gateway terminates and verifies JWTs at the network boundary.
   - Downstream services receive verified identity claims through HTTP headers: `X-User-Id`, `X-User-Role`, and `X-User-Name`.
   - Downstream services (`account-service`, `payment-service`) enforce resource ownership locally without calling an external auth service.
4. **Gateway Rate Limiting**:
   - Apply rate limiting on `POST /auth/login` to prevent credential brute-forcing. For local/free tier, an in-memory bucket limiter is implemented, with documentation on migrating to distributed Redis in large-scale production.

## Consequences
- **Positive**: Cryptographic isolation, zero credentials stored downstream, short token exposure window, automatic replay detection on refresh tokens, and strict resource isolation per customer.
- **Trade-off**: Revocation of an active access token cannot be immediate without a distributed blacklist; mitigated by keeping access token lifetime short (15 minutes).
