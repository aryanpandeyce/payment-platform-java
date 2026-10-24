# scripts/scan.ps1
# Executes code quality and security scans locally (SonarQube + Trivy)

$ErrorActionPreference = "Continue"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Running ApexPay DevSecOps & Security Audit Scan        " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. SonarQube Quality Gate Scan
if ($env:SONAR_TOKEN) {
    Write-Host "[1/4] Running SonarQube Maven Analysis..." -ForegroundColor Cyan
    .\mvnw.cmd sonar:sonar `
      -Dsonar.host.url="http://localhost:9000" `
      -Dsonar.token="$env:SONAR_TOKEN" `
      -Dsonar.qualitygate.wait=true
} else {
    Write-Host "[1/4] SONAR_TOKEN not set. Skipping SonarQube analysis (see docs/devsecops.md)." -ForegroundColor Yellow
}

# 2. Trivy Filesystem Vulnerability Scan
if (Get-Command trivy -ErrorAction SilentlyContinue) {
    Write-Host "[2/4] Running Trivy Filesystem Vulnerability Scan..." -ForegroundColor Cyan
    trivy fs --scanners vuln --severity HIGH,CRITICAL --ignorefile .trivyignore .

    Write-Host "[3/4] Running Trivy Secret Leak Detection..." -ForegroundColor Cyan
    trivy fs --scanners secret .

    Write-Host "[4/4] Running Trivy IaC / Kubernetes Misconfig Scan..." -ForegroundColor Cyan
    trivy config deploy/
} else {
    Write-Host "[INFO] 'trivy' CLI not found. Running Trivy container via Docker..." -ForegroundColor Yellow
    docker run --rm -v ${PWD}:/apps -w /apps aquasec/trivy:0.60.0 fs --scanners vuln --severity HIGH,CRITICAL --ignorefile .trivyignore .
    docker run --rm -v ${PWD}:/apps -w /apps aquasec/trivy:0.60.0 fs --scanners secret .
    docker run --rm -v ${PWD}:/apps -w /apps aquasec/trivy:0.60.0 config deploy/
}

Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  Security Scans Completed! Check output for details.     " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
