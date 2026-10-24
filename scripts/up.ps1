# scripts/up.ps1
# Starts ApexPay microservices platform via Docker Compose

param (
    [switch]$WithObservability,
    [switch]$WithQuality
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Starting ApexPay Platform via Docker Compose            " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$composeFiles = @("-f", "docker-compose.yml")

if ($WithObservability) {
    Write-Host "[INFO] Including Observability Stack (Prometheus, Grafana, ELK)..." -ForegroundColor Yellow
    $composeFiles += @("-f", "docker-compose.observability.yml")
}

if ($WithQuality) {
    Write-Host "[INFO] Including Code Quality Stack (SonarQube)..." -ForegroundColor Yellow
    $composeFiles += @("-f", "docker-compose.quality.yml")
}

docker compose $composeFiles up --build -d

Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  ApexPay Platform is UP!                                 " -ForegroundColor Green
Write-Host "  Frontend: http://localhost:5173                         " -ForegroundColor Green
Write-Host "  Gateway:  http://localhost:8090                         " -ForegroundColor Green
if ($WithObservability) {
    Write-Host "  Grafana:  http://localhost:3000 (admin / admin)         " -ForegroundColor Green
    Write-Host "  Kibana:   http://localhost:5601                         " -ForegroundColor Green
    Write-Host "  Prom:     http://localhost:9090                         " -ForegroundColor Green
}
Write-Host "==========================================================" -ForegroundColor Green
