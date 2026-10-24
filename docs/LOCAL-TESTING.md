# Local Testing Guide (PowerShell)

This guide provides the exact PowerShell commands to test the complete event-driven payment platform locally.

All commands target the **API Gateway** on port `8090`, which validates JWT tokens and routes requests to the internal microservices (`account-service:8085` and `payment-service:8086`).

---

## 1. Obtain JWT Authentication Token

Authenticate with the API Gateway demo credentials:

```powershell
$loginBody = @{
    username = "user1"
    password = "password123"
} | ConvertTo-Json

$authResponse = Invoke-RestMethod -Uri "http://localhost:8090/auth/login" `
    -Method Post `
    -Body $loginBody `
    -ContentType "application/json"

$token = $authResponse.token
$headers = @{
    "Authorization" = "Bearer $token"
}

Write-Host "JWT Token acquired: $($token.Substring(0, 30))..." -ForegroundColor Green
```

---

## 2. Create Two Accounts (Rahul & Priya)

Create two test accounts using the exact JSON fields (`ownerName`, `initialBalance`, `currency`):

```powershell
# Create Rahul's Account (₹10,000)
$rahulReq = @{
    ownerName      = "Rahul Sharma"
    initialBalance = 10000.00
    currency       = "INR"
} | ConvertTo-Json

$rahul = Invoke-RestMethod -Uri "http://localhost:8090/api/accounts" `
    -Method Post `
    -Headers $headers `
    -Body $rahulReq `
    -ContentType "application/json"

$rahulId = $rahul.id
Write-Host "Rahul's Account Created -> ID: $rahulId | Balance: ₹$($rahul.balance)" -ForegroundColor Green

# Create Priya's Account (₹5,000)
$priyaReq = @{
    ownerName      = "Priya Patel"
    initialBalance = 5000.00
    currency       = "INR"
} | ConvertTo-Json

$priya = Invoke-RestMethod -Uri "http://localhost:8090/api/accounts" `
    -Method Post `
    -Headers $headers `
    -Body $priyaReq `
    -ContentType "application/json"

$priyaId = $priya.id
Write-Host "Priya's Account Created -> ID: $priyaId | Balance: ₹$($priya.balance)" -ForegroundColor Green
```

---

## 3. Send a Valid Payment (Rahul -> Priya: ₹500)

Initiate a transfer using the required `Idempotency-Key` header and exact fields (`senderAccountId`, `receiverAccountId`, `amount`, `currency`):

```powershell
# Generate a unique Idempotency Key
$idempotencyKey1 = [System.Guid]::NewGuid().ToString()

$payHeaders1 = @{
    "Authorization"   = "Bearer $token"
    "Idempotency-Key" = $idempotencyKey1
}

$payBody1 = @{
    senderAccountId   = $rahulId
    receiverAccountId = $priyaId
    amount            = 500.00
    currency          = "INR"
} | ConvertTo-Json

$payment1 = Invoke-RestMethod -Uri "http://localhost:8090/api/payments" `
    -Method Post `
    -Headers $payHeaders1 `
    -Body $payBody1 `
    -ContentType "application/json"

$payment1Id = $payment1.id
Write-Host "Payment Initiated -> ID: $payment1Id | Status: $($payment1.status)" -ForegroundColor Cyan

# Wait 2 seconds for Kafka event flow:
# payment-service (Outbox) -> fraud-service -> account-service (settlement) -> payment-service
Start-Sleep -Seconds 2

# Verify settled status
$settledCheck = Invoke-RestMethod -Uri "http://localhost:8090/api/payments/$payment1Id" `
    -Method Get `
    -Headers $headers

Write-Host "Final Payment Status: $($settledCheck.status)" -ForegroundColor Green
# Expected: SETTLED
```

---

## 4. Idempotency Test: Repeat with the SAME Idempotency-Key

Re-send the **exact same request** using the same `$idempotencyKey1`:

```powershell
$retryPayment = Invoke-RestMethod -Uri "http://localhost:8090/api/payments" `
    -Method Post `
    -Headers $payHeaders1 `
    -Body $payBody1 `
    -ContentType "application/json"

Write-Host "Idempotent Replay -> Payment ID: $($retryPayment.id) | Status: $($retryPayment.status)" -ForegroundColor Green
Write-Host "Matched Original ID: $($retryPayment.id -eq $payment1Id)" -ForegroundColor Green
# Expected: True (Same payment returned, no duplicate debit occurred)
```

---

## 5. Fraud Detection Test: Send a ₹60,000 Payment (Fraud Fail)

The fraud-service enforces a single-transaction limit of ₹50,000 (`fraud.max-amount`).
Send a payment exceeding this threshold:

```powershell
$fraudKey = [System.Guid]::NewGuid().ToString()

$fraudHeaders = @{
    "Authorization"   = "Bearer $token"
    "Idempotency-Key" = $fraudKey
}

$fraudBody = @{
    senderAccountId   = $rahulId
    receiverAccountId = $priyaId
    amount            = 60000.00
    currency          = "INR"
} | ConvertTo-Json

$fraudPayment = Invoke-RestMethod -Uri "http://localhost:8090/api/payments" `
    -Method Post `
    -Headers $fraudHeaders `
    -Body $fraudBody `
    -ContentType "application/json"

$fraudPaymentId = $fraudPayment.id
Write-Host "Large Payment Submitted -> ID: $fraudPaymentId | Status: $($fraudPayment.status)" -ForegroundColor Cyan

# Wait 2 seconds for fraud evaluation and Kafka topic routing
Start-Sleep -Seconds 2

$fraudResult = Invoke-RestMethod -Uri "http://localhost:8090/api/payments/$fraudPaymentId" `
    -Method Get `
    -Headers $headers

Write-Host "Status after Fraud Evaluation: $($fraudResult.status)" -ForegroundColor Red
Write-Host "Rejection Reason:             $($fraudResult.failureReason)" -ForegroundColor Yellow
# Expected Status: FAILED
# Expected Reason: Transaction amount exceeds maximum allowed limit
```

---

## 6. Check Balances

Verify that Rahul was debited exactly ₹500 (and not double-debited by the retry, nor debited by the failed ₹60,000 transfer), and Priya was credited exactly ₹500:

```powershell
# Rahul's Balance: Started with ₹10,000 - ₹500 = ₹9,500.00
$rahulBalance = Invoke-RestMethod -Uri "http://localhost:8090/api/accounts/$rahulId/balance" `
    -Method Get `
    -Headers $headers

# Priya's Balance: Started with ₹5,000 + ₹500 = ₹5,500.00
$priyaBalance = Invoke-RestMethod -Uri "http://localhost:8090/api/accounts/$priyaId/balance" `
    -Method Get `
    -Headers $headers

Write-Host "`n================ LEDGER VERIFICATION ================" -ForegroundColor Cyan
Write-Host "Rahul's Final Balance: ₹$($rahulBalance.balance) (Expected: 9500.00)" -ForegroundColor $(if ($rahulBalance.balance -eq 9500) { "Green" } else { "Red" })
Write-Host "Priya's Final Balance: ₹$($priyaBalance.balance) (Expected: 5500.00)" -ForegroundColor $(if ($priyaBalance.balance -eq 5500) { "Green" } else { "Red" })
Write-Host "======================================================`n" -ForegroundColor Cyan
```

---

## 7. Inspect MongoDB Audit Trail

Verify that all transaction events (`payment-initiated`, `payment-validated`, `payment-settled`, `payment-failed`) were captured in MongoDB:

```powershell
docker compose exec mongodb mongosh -u audit -p change_me --authenticationDatabase admin --eval "
  db = db.getSiblingDB('audit_db');
  print('Total Audited Event Documents: ' + db.event_logs.countDocuments());
  db.event_logs.find({}, {eventType: 1, paymentId: 1, timestamp: 1, _id: 0}).pretty();
"
```
