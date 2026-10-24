# scripts/test.ps1
# Runs unit and integration test suite across all 8 modules

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Running ApexPay Multi-Module Test Suite                 " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

.\mvnw.cmd clean test -B

if ($LASTEXITCODE -eq 0) {
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host "  ALL TESTS PASSED!                                       " -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Test suite failed with exit code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}
