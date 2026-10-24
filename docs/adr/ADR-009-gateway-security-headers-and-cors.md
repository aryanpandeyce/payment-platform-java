# ADR-009: API Gateway Security Headers, CORS, and Vulnerability Scanning

## Status
Accepted

## Context
Exposing financial microservices directly to the web presents several security attack surfaces:
1. **Cross-Origin Resource Sharing (CORS)**: An overly permissive wildcard (`Access-Control-Allow-Origin: *`) allows unauthorized third-party websites to forge credentialed requests against the banking platform.
2. **Browser Exploitation Vectors**: Missing defensive HTTP response headers expose client browsers to MIME-sniffing attacks, clickjacking via iframes, and cross-site scripting (XSS).
3. **Supply Chain Vulnerabilities**: Open-source libraries in the Maven and npm dependency graphs can harbor known CVEs and transitive security flaws.

## Decision
1. **Defensive HTTP Response Headers**:
   Spring Cloud Gateway enforces five default security headers on all responses forwarded to clients:
   - `X-Content-Type-Options: nosniff` (prevents MIME-type confusion attacks)
   - `X-Frame-Options: DENY` (prevents clickjacking attacks)
   - `X-XSS-Protection: 1; mode=block` (activates legacy browser XSS filters)
   - `Referrer-Policy: strict-origin-when-cross-origin` (protects URI parameters in outbound links)
   - `Strict-Transport-Security: max-age=31536000; includeSubDomains` (enforces HTTPS)
2. **Strict CORS Policy**:
   Allowed origins are restricted to configured client hosts via `CORS_ALLOWED_ORIGINS` (defaulting to the React dashboard `http://localhost:5173`). Wildcard `*` is strictly prohibited in production.
3. **Software Composition Analysis (SCA) & Quality Gates**:
   - Automated weekly updates managed by Dependabot (`.github/dependabot.yml`).
   - Trivy vulnerability scanner runs against dependencies and filesystem with zero CRITICAL/HIGH tolerance.
   - SonarQube quality gate enforces:
     - New Code Coverage >= 70%
     - 0 New Bugs and 0 New Vulnerabilities
     - Duplication <= 3%
     - 100% Security Hotspots Reviewed

## Consequences & Trade-offs
- **Positive**:
  - Eliminates browser attack vectors out of the box.
  - Guarantees automated alerts and dependency patches for newly discovered CVEs.
- **Negative / Trade-offs**:
  - Developers integrating new frontends or external API consumers must explicitly append their origin to `CORS_ALLOWED_ORIGINS`.
