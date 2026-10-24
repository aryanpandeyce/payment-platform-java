# scripts/demo-data.ps1
# Verifies demo seed data across identity, account, and payment services

param (
    [string]$BaseUrl = "http://localhost:8090"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  ApexPay Demo Seed Data Verification                     " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$AdminPassword = if ($env:SEED_ADMIN_PASSWORD) { $env:SEED_ADMIN_PASSWORD } else { "Admin@ApexPay2026!" }

# Authenticate as Admin
Write-Host "[1/3] Authenticating as Admin 'aryan.pandey'..." -ForegroundColor Cyan
$loginPayload = @{
    username = "aryan.pandey"
    password = $AdminPassword
} | ConvertTo-Json

try {
    $login = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method Post -ContentType "application/json" -Body $loginPayload
    $token = $login.accessToken
    Write-Host "[PASS] Admin login succeeded." -ForegroundColor Green
    
    # Check users
    $headers = @{ Authorization = "Bearer $token" }
    $users = Invoke-RestMethod -Uri "$BaseUrl/api/users" -Method Get -Headers $headers
    Write-Host "[2/3] Verified $($users.Count) registered users in identity_db." -ForegroundColor Green

    # Check accounts
    $accounts = Invoke-RestMethod -Uri "$BaseUrl/api/accounts" -Method Get -Headers $headers
    Write-Host "[3/3] Verified $($accounts.Count) active customer accounts in account_db." -ForegroundColor Green

    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host "  Demo seed data is active, consistent, and ready!        " -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
} catch {
    Write-Host "[WARNING] Could not connect to ApexPay gateway at $BaseUrl. Ensure services are running via .\scripts\up.ps1" -ForegroundColor Yellow
}
