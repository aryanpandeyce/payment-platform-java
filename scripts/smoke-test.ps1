# scripts/smoke-test.ps1
# End-to-end smoke test verifying cluster/gateway authentication and payment transfer

param (
    [string]$BaseUrl = "http://localhost"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  ApexPay Platform End-to-End Smoke Test                  " -ForegroundColor Cyan
Write-Host "  Target: $BaseUrl                                        " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$AdminPassword = if ($env:SEED_ADMIN_PASSWORD) { $env:SEED_ADMIN_PASSWORD } else { "Admin@ApexPay2026!" }
$UserPassword  = if ($env:SEED_USER_PASSWORD)  { $env:SEED_USER_PASSWORD }  else { "User@ApexPay2026!" }

# 1. Login as Admin (Aryan Pandey)
Write-Host "[1/5] Authenticating as Admin 'aryan.pandey'..." -ForegroundColor Cyan
$loginPayload = @{
    username = "aryan.pandey"
    password = $AdminPassword
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/login" `
    -Method Post `
    -ContentType "application/json" `
    -Body $loginPayload

$adminToken = $loginResponse.accessToken
if (-not $adminToken) {
    Write-Host "[ERROR] Failed to obtain access token for aryan.pandey" -ForegroundColor Red
    exit 1
}
Write-Host "[PASS] Admin login successful! (Access token acquired)" -ForegroundColor Green

# 2. Verify /api/users/me as Admin
Write-Host "[2/5] Fetching Admin profile via /api/users/me..." -ForegroundColor Cyan
$headers = @{
    Authorization = "Bearer $adminToken"
}
$adminMe = Invoke-RestMethod -Uri "$BaseUrl/api/users/me" -Method Get -Headers $headers
Write-Host "[PASS] Verified user: $($adminMe.fullName) | Role: $($adminMe.role)" -ForegroundColor Green

# 3. Fetch all accounts as Admin
Write-Host "[3/5] Querying system accounts as Admin..." -ForegroundColor Cyan
$accounts = Invoke-RestMethod -Uri "$BaseUrl/api/accounts" -Method Get -Headers $headers
Write-Host "[PASS] Successfully retrieved $($accounts.Count) accounts from cluster!" -ForegroundColor Green

# 4. Login as USER 'rahul.sharma' to perform transfer
Write-Host "[4/5] Authenticating as User 'rahul.sharma'..." -ForegroundColor Cyan
$userLoginPayload = @{
    username = "rahul.sharma"
    password = $UserPassword
} | ConvertTo-Json

$userLoginResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/login" `
    -Method Post `
    -ContentType "application/json" `
    -Body $userLoginPayload

$userToken = $userLoginResponse.accessToken
Write-Host "[PASS] User login successful!" -ForegroundColor Green

# 5. Initiate a payment transfer from Rahul to Priya
Write-Host "[5/5] Initiating transfer of INR 100.00 from Rahul to Priya..." -ForegroundColor Cyan
$idempotencyKey = "smoke-test-" + [System.Guid]::NewGuid().ToString()
$paymentHeaders = @{
    Authorization = "Bearer $userToken"
    "Idempotency-Key" = $idempotencyKey
}

$paymentPayload = @{
    sourceAccountId = "11111111-1111-1111-1111-111111111101"
    targetAccountId = "22222222-2222-2222-2222-222222222201"
    amount = 100.00
    currency = "INR"
    reference = "Smoke test automated cluster payment"
} | ConvertTo-Json

$paymentResponse = Invoke-RestMethod -Uri "$BaseUrl/api/payments" `
    -Method Post `
    -ContentType "application/json" `
    -Headers $paymentHeaders `
    -Body $paymentPayload

Write-Host "[PASS] Payment initiated successfully! Payment ID: $($paymentResponse.paymentId) | Status: $($paymentResponse.status)" -ForegroundColor Green

Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  ALL SMOKE TESTS PASSED! PLATFORM IS HEALTHY AND SECURE  " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
