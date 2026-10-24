# scripts/deploy.ps1
# Deploys ApexPay platform to the active Kubernetes cluster (Kind / Kube context)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Deploying ApexPay to Kubernetes Cluster                 " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Check if Helm is available locally; fallback to docker alpine/helm or kubectl
$hasHelm = (Get-Command helm -ErrorAction SilentlyContinue)

if ($hasHelm) {
    Write-Host "[INFO] Deploying via local Helm CLI..." -ForegroundColor Cyan
    helm upgrade --install apexpay deploy/helm/payment-platform `
        -f deploy/helm/payment-platform/values-dev.yaml `
        --create-namespace `
        --namespace apexpay
} else {
    Write-Host "[INFO] Local 'helm' CLI not found. Using Docker alpine/helm to deploy..." -ForegroundColor Yellow
    docker run --rm -v ${PWD}:/apps -v $HOME/.kube:/root/.kube -w /apps alpine/helm:3.17.1 `
        upgrade --install apexpay deploy/helm/payment-platform `
        -f deploy/helm/payment-platform/values-dev.yaml `
        --create-namespace `
        --namespace apexpay
}

Write-Host "[INFO] Waiting for deployments to become ready (max 180s)..." -ForegroundColor Cyan
kubectl rollout status deployment/api-gateway -n apexpay --timeout=180s
kubectl rollout status deployment/identity-service -n apexpay --timeout=180s
kubectl rollout status deployment/account-service -n apexpay --timeout=180s
kubectl rollout status deployment/payment-service -n apexpay --timeout=180s

Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  ApexPay Platform successfully deployed!                 " -ForegroundColor Green
Write-Host "  Access via Ingress: http://localhost                   " -ForegroundColor Green
Write-Host "  Run .\scripts\smoke-test.ps1 to verify end-to-end.     " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
