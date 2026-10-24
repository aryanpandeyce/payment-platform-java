# scripts/kind-down.ps1
# Tears down the local Kind cluster and cleans up resources

$ErrorActionPreference = "Continue"

Write-Host "==========================================================" -ForegroundColor Yellow
Write-Host "  Tearing down ApexPay Kind Cluster                       " -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Yellow

if (Get-Command kind -ErrorAction SilentlyContinue) {
    kind delete cluster --name apexpay-cluster
    Write-Host "[INFO] Cluster apexpay-cluster successfully deleted." -ForegroundColor Green
} else {
    Write-Host "[WARNING] 'kind' CLI not found. If running in Docker, remove container directly." -ForegroundColor Yellow
}
