# 2-Minute End-to-End Demo Script

This script walks through the complete event-driven payment platform in 2 minutes using PowerShell.

---

## 0. Prerequisites & Setup

Ensure all microservices and infrastructure are running:

```powershell
# Option A: Run everything with Docker Compose
docker compose up -d

# Verify containers are healthy
docker compose ps
```

---

## 1. Authentication (API Gateway on Port 8090)

Authenticate with the API Gateway to obtain a JWT token:

```powershell
$loginBody = @{
    username = "user1"
    password = "password123"
} | ConvertTo-Json

$authResponse = Invoke-RestMethod -Uri "http://localhost:8090/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $authResponse.token
$headers = @{
    "Authorization" = "Bearer $token"
}

Write-Host "Obtained JWT Token: $($token.Substring(0, 25))..."
```

---

## 2. Create Accounts (Rahul & Priya)

Create Rahul's account with ₹10,000 and Priya's account with ₹5,000:

```powershell
# Create Rahul's Account
$rahulBody = @{
    name = "Rahul Sharma"
    email = "rahul@example.com"
    initialBalance = 10000.00
} | ConvertTo-Json

$rahul = Invoke-RestMethod -Uri "http://localhost:8090/api/accounts" -Method Post -Headers $headers -Body $rahulBody -ContentType "application/json"
$rahulId = $rahul.id
Write-Host "Rahul's Account Created with ID: $rahulId, Balance: ₹$($rahul.balance)"

# Create Priya's Account
$priyaBody = @{
    name = "Priya Patel"
    email = "priya@example.com"
    initialBalance = 5000.00
} | ConvertTo-Json

$priya = Invoke-RestMethod -Uri "http://localhost:8090/api/accounts" -Method Post -Headers $headers -Body $priyaBody -ContentType "application/json"
$priyaId = $priya.id
Write-Host "Priya's Account Created with ID: $priyaId, Balance: ₹$($priya.balance)"
```

---

## 3. Happy Path: Rahul Sends ₹500 to Priya

Generate an `Idempotency-Key` and initiate the transfer:

```powershell
$idempotencyKey1 = [System.Guid]::NewGuid().ToString()

$paymentHeaders = @{
    "Authorization" = "Bearer $token"
    "Idempotency-Key" = $idempotencyKey1
}

$paymentBody = @{
    fromAccountId = $rahulId
    toAccountId = $priyaId
    amount = 500.00
    currency = "INR"
} | ConvertTo-Json

$payment = Invoke-RestMethod -Uri "http://localhost:8090/api/payments" -Method Post -Headers $paymentHeaders -Body $paymentBody -ContentType "application/json"
$paymentId = $payment.id
Write-Host "Payment Initiated! Payment ID: $paymentId, Status: $($payment.status)"

# Wait 2 seconds for Kafka event choreography:
# Outbox Poller -> payment-initiated -> Fraud Service -> payment-validated -> Account Service (Settlement) -> payment-settled -> Payment Service & Notification Service
Start-Sleep -Seconds 2

# Check updated payment status
$settledPayment = Invoke-RestMethod -Uri "http://localhost:8090/api/payments/$paymentId" -Method Get -Headers $headers
Write-Host "Final Payment Status: $($settledPayment.status)"  # Expected: SETTLED

# Check updated balances
$rahulBalance = Invoke-RestMethod -Uri "http://localhost:8090/api/accounts/$rahulId/balance" -Method Get -Headers $headers
$priyaBalance = Invoke-RestMethod -Uri "http://localhost:8090/api/accounts/$priyaId/balance" -Method Get -Headers $headers
Write-Host "Rahul's New Balance: ₹$($rahulBalance.balance)"  # Expected: 9500.00
Write-Host "Priya's New Balance: ₹$($priyaBalance.balance)"  # Expected: 5500.00
```

---

## 4. Idempotency Test: Network Retry Protection

Resend the exact same request with the same `Idempotency-Key`:

```powershell
# Exact same key + body should return the exact same payment record (no double debit!)
$retryPayment = Invoke-RestMethod -Uri "http://localhost:8090/api/payments" -Method Post -Headers $paymentHeaders -Body $paymentBody -ContentType "application/json"
Write-Host "Idempotent Retry Returned Payment ID: $($retryPayment.id), Status: $($retryPayment.status)"

# Verify balances didn't change:
$rahulBalanceCheck = Invoke-RestMethod -Uri "http://localhost:8090/api/accounts/$rahulId/balance" -Method Get -Headers $headers
Write-Host "Rahul's Balance after retry (no double charge): ₹$($rahulBalanceCheck.balance)"  # Still 9500.00
```

---

## 5. Fraud Detection: Velocity & Limit Breach

Attempt a payment exceeding the single-transaction fraud threshold (₹50,000):

```powershell
$fraudKey = [System.Guid]::NewGuid().ToString()
$fraudHeaders = @{
    "Authorization" = "Bearer $token"
    "Idempotency-Key" = $fraudKey
}

$fraudBody = @{
    fromAccountId = $rahulId
    toAccountId = $priyaId
    amount = 75000.00
    currency = "INR"
} | ConvertTo-Json

$fraudPayment = Invoke-RestMethod -Uri "http://localhost:8090/api/payments" -Method Post -Headers $fraudHeaders -Body $fraudBody -ContentType "application/json"
Write-Host "Large Payment Submitted: $($fraudPayment.id), Status: $($fraudPayment.status)"

# Wait for Fraud Service evaluation
Start-Sleep -Seconds 2

$fraudResult = Invoke-RestMethod -Uri "http://localhost:8090/api/payments/$($fraudPayment.id)" -Method Get -Headers $headers
Write-Host "Payment Status after Fraud Check: $($fraudResult.status)"  # Expected: FAILED
Write-Host "Failure Reason: $($fraudResult.failureReason)"           # Expected: Transaction amount exceeds maximum allowed limit
```

---

## 6. Audit Trail in MongoDB

Inspect the append-only event audit logs captured in MongoDB:

```powershell
docker compose exec mongodb mongosh -u audit -p change_me --authenticationDatabase admin --eval "
  db = db.getSiblingDB('audit_db');
  print('Total Audited Events: ' + db.event_logs.countDocuments());
  db.event_logs.find({}, {eventType: 1, paymentId: 1, timestamp: 1, _id: 0}).limit(5).pretty();
"
```

---

## 7. Frontend Live Dashboard

1. Open your browser and navigate to `http://localhost:5173`.
2. Login with username `user1` and password `password123`.
3. Notice:
   - Live transaction polling every 2 seconds.
   - Quick balance indicators for Rahul & Priya.
   - Instant Idempotency-Key generation preventing double-clicks.
   - Responsive status transitions: `INITIATED` ⏳ $\rightarrow$ `VALIDATED` 🔍 $\rightarrow$ `SETTLED` ✅ / `FAILED` ❌.
