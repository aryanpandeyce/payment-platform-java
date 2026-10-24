# ADR-008: Dual CI Pipeline Architecture (GitHub Actions and GitLab CI)

## Status
Accepted

## Context
Payment-platform-java is hosted as an open-source demonstration on GitHub while also requiring enterprise-grade DevSecOps capabilities for resume and portfolio evaluation:
1. **GitHub Ecosystem**: The public repository resides on GitHub, where viewers expect standard repository status badges (build green, GitHub Pages live demo link). GitHub Pages requires GitHub Actions to deploy static assets.
2. **GitLab DevSecOps Strengths**: Enterprise banking and payment roles frequently evaluate comprehensive GitLab CI/CD pipelines featuring native Container Registry, built-in Security Dashboards, Dependency Scanning, Secret Detection, SAST, and automated Helm deployment stages.
3. **Redundancy vs. Portability**: A pure GitHub Actions setup lacks native GitLab container registry integration and enterprise runner features. Conversely, a pure GitLab setup breaks public GitHub repository status badges and GitHub Pages.

## Decision
We implement a **Dual CI/CD Pipeline Architecture**:
1. **GitHub Actions (`.github/workflows/ci.yml`)**:
   - Executes multi-module build and unit test verification using Java 21 across all microservices.
   - Builds and deploys the React frontend in mock demonstration mode to GitHub Pages.
   - Provides public repository status badges.
2. **GitLab CI (`.gitlab-ci.yml`)**:
   - Represents the enterprise DevSecOps standard featuring 6 sequential stages:
     `build` -> `test` -> `quality` -> `package` -> `security` -> `deploy`.
   - **build/test**: Maven compilation and test execution with `.m2` caching, JaCoCo coverage reports, and Surefire JUnit report publishing.
   - **quality**: SonarQube analysis with strict quality gate gating (`sonar.qualitygate.wait=true`).
   - **package**: Container images built with Kaniko/DinD, tagged with commit SHA and `latest` on main, and pushed to the GitLab Container Registry.
   - **security**: Trivy container vulnerability scan (failing on HIGH/CRITICAL), Trivy filesystem & IaC Helm audit, and secret detection.
   - **deploy**: Automated/manual `helm upgrade --install` to local or remote Kubernetes clusters (kind).

## Consequences & Trade-offs
- **Positive**:
  - The repository maintains full public showcase visibility on GitHub while demonstrating complete enterprise GitLab CI/CD mastery.
  - Zero lock-in: either CI system can build and verify the entire project independently.
- **Negative / Trade-offs**:
  - Requires maintaining workflow definitions across two CI platforms; mitigated by relying on standard Maven, Docker, Trivy, and Helm CLI commands that execute identically across both engines.
