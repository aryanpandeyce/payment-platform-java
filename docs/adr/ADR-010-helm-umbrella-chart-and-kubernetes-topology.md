# ADR-010: Helm Umbrella Chart and Kubernetes Deployment Topology

## Status
Accepted

## Context
Deploying the payment system to container orchestration platforms (local Kubernetes kind clusters and enterprise OpenShift / EKS / GKE clusters) requires scalable, templated configuration management:
1. **Raw Manifest Proliferation**: Managing individual raw YAML files across 7 microservices and 3 databases results in copy-paste errors and drift across environments.
2. **Configuration Drift**: Dev and Prod environments require different scaling, resource limits, and secrets management.
3. **Enterprise OpenShift Constraints**: Enterprise financial banking environments enforce strict Security Context Constraints (`restricted` / `restricted-v2` SCC). Images with fixed UIDs (`runAsUser: 0`), root capabilities, or privilege escalation are rejected at admission time.
4. **Zero-Trust Network Isolation**: All pods should not have unrestricted network access to each other; microservices must only communicate with their designated dependencies.

## Decision
1. **Helm Umbrella Chart (`deploy/helm/payment-platform`)**:
   - Manages all 7 services (`identity-service`, `account-service`, `payment-service`, `fraud-service`, `notification-service`, `api-gateway`, `frontend`) as sub-components.
   - Declarative values files: `values-dev.yaml` (lightweight single-replica development) and `values-prod-like.yaml` (multi-replica, HPA autoscaling, PodDisruptionBudgets).
2. **Kubernetes Primitives**:
   - **Deployments**: With rolling update strategy, liveness & readiness Actuator HTTP probes, and explicit resource requests & limits.
   - **Services**: Internal ClusterIP services with semantic DNS naming (`http://account-service:8085`).
   - **Ingress**: `ingress-nginx` routing `/auth` and `/api` to `api-gateway`, and `/` to `frontend`.
   - **HorizontalPodAutoscaler (HPA)**: Target 75% CPU utilization.
   - **PodDisruptionBudget (PDB)**: `minAvailable: 1` ensuring high availability during node drain operations.
3. **OpenShift `restricted-v2` SCC Compliance**:
   - `runAsNonRoot: true`
   - `allowPrivilegeEscalation: false`
   - `capabilities: drop: ["ALL"]`
   - `seccompProfile: type: "RuntimeDefault"`
   - `readOnlyRootFilesystem: true` with an emptyDir volume mounted at `/tmp` for temporary JVM writes.
   - Arbitrary UID tolerance: JVM processes work without assuming a fixed UID.
4. **Zero-Trust NetworkPolicy**:
   - `default-deny-all` ingress NetworkPolicy for the namespace.
   - Explicit ingress rules:
     - Ingress -> Gateway (8090) & Frontend (80)
     - Gateway -> Services (8084, 8085, 8086, 8088)
     - Services -> PostgreSQL (5432), Kafka (9092), MongoDB (27017)
5. **Raw Manifest Export**:
   - Rendered static YAML manifests provided in `deploy/k8s/` so auditors can inspect the cluster state without installing Helm.

## Consequences & Trade-offs
- **Positive**:
  - Reproducible cluster deployment with a single command (`helm upgrade --install`).
  - Seamless compliance with OpenShift and hardened cloud Kubernetes security benchmarks.
- **Negative / Trade-offs**:
  - Running a local kind cluster requires ~4-6GB RAM and Docker running on the host machine.
