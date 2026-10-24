# scripts/kind-up.ps1
# Spins up local Kind cluster with NGINX ingress and loads ApexPay images

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Spinning up ApexPay Kind Cluster with Ingress-NGINX    " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Verify Kind is installed
if (-not (Get-Command kind -ErrorAction SilentlyContinue)) {
    Write-Host "[INFO] 'kind' CLI not found in PATH." -ForegroundColor Yellow
    Write-Host "Please install kind via: choco install kind / winget install Kubernetes.kind" -ForegroundColor Yellow
    Write-Host "Or download from: https://kind.sigs.k8s.io/docs/user/quick-start/#installation" -ForegroundColor Yellow
    exit 1
}

# 2. Check if cluster already exists
$clusterList = kind get clusters
if ($clusterList -contains "apexpay-cluster") {
    Write-Host "[INFO] Cluster 'apexpay-cluster' already exists. Skipping creation." -ForegroundColor Green
} else {
    Write-Host "[INFO] Creating Kind cluster using kind-config.yaml..." -ForegroundColor Cyan
    kind create cluster --config kind-config.yaml
}

# 3. Install NGINX Ingress Controller for Kind
Write-Host "[INFO] Installing Ingress-NGINX controller..." -ForegroundColor Cyan
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

Write-Host "[INFO] Waiting for ingress-nginx controller to become ready (max 120s)..." -ForegroundColor Cyan
kubectl wait --namespace ingress-nginx `
  --for=condition=ready pod `
  --selector=app.kubernetes.io/component=controller `
  --timeout=120s

# 4. Build and load Docker images
Write-Host "[INFO] Loading ApexPay images into Kind cluster..." -ForegroundColor Cyan
$services = @("identity-service", "account-service", "payment-service", "fraud-service", "notification-service", "api-gateway", "frontend")

foreach ($svc in $services) {
    $img = "apexpay/$svc`:latest"
    Write-Host "[INFO] Loading image $img into Kind..." -ForegroundColor Gray
    kind load docker-image $img --name apexpay-cluster
}

Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  Kind Cluster is READY! Run .\scripts\deploy.ps1 next.  " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
