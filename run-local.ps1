<#
.SYNOPSIS
    Local Run Helper for Payment Platform Microservices.
.DESCRIPTION
    1. Loads environment variables (.env or existing env).
    2. Configures JAVA_HOME and Asia/Kolkata timezone.
    3. Starts Docker infrastructure (PostgreSQL, Kafka, MongoDB) and waits until ready.
    4. Launches all 5 Java microservices in separate PowerShell windows.
    5. Prints health-check URLs and instructions.
#>

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   Payment Platform - Local Development Environment       " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Environment & JAVA_HOME Configuration (No hardcoded paths)
if ($env:JAVA_HOME) {
    Write-Host "[OK] Using existing JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Green
} else {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        Write-Host "[INFO] JAVA_HOME not explicitly set; using 'java' found at: $($javaCmd.Source)" -ForegroundColor Yellow
    } else {
        Write-Warning "JAVA_HOME is not set and java was not found in PATH. Please ensure Java 21 is installed."
    }
}

# Set timezone required for PostgreSQL timestamp compatibility
$env:JAVA_TOOL_OPTIONS = "-Duser.timezone=Asia/Kolkata"
Write-Host "[OK] Set JAVA_TOOL_OPTIONS = -Duser.timezone=Asia/Kolkata" -ForegroundColor Green

# 2. Load .env if present (without committing secrets)
$envFilePath = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFilePath) {
    Write-Host "[OK] Loading environment variables from .env..." -ForegroundColor Green
    Get-Content $envFilePath | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
            $parts = $line.Split('=', 2)
            $k = $parts[0].Trim()
            $v = $parts[1].Trim().Trim('"').Trim("'")
            [System.Environment]::SetEnvironmentVariable($k, $v, [System.EnvironmentVariableTarget]::Process)
        }
    }
}

# Database and Broker Configuration Defaults
$dbHost = if ($env:POSTGRES_HOST) { $env:POSTGRES_HOST } else { "localhost" }
$dbPort = if ($env:POSTGRES_PORT) { $env:POSTGRES_PORT } else { "5433" }
$dbUser = if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { "payments" }
$dbPass = if ($env:POSTGRES_PASSWORD) { $env:POSTGRES_PASSWORD } else { "root" }
$kafkaHost = if ($env:KAFKA_BOOTSTRAP_SERVERS) { $env:KAFKA_BOOTSTRAP_SERVERS } else { "localhost:9092" }
$mongoUri = if ($env:MONGO_URI) { $env:MONGO_URI } else { "mongodb://audit:change_me@localhost:27017/audit_db?authSource=admin" }
$jwtSecret = if ($env:JWT_SECRET) { $env:JWT_SECRET } else { "dGVzdC1zZWNyZXQta2V5LXNob3VsZC1iZS1hdC1sZWFzdC0yNTYtYml0cy1sb25nLWZvci1obWFj" }

# 3. Start Infrastructure via Docker Compose
Write-Host "`n[1/3] Starting Docker infrastructure (PostgreSQL, Kafka, MongoDB)..." -ForegroundColor Cyan
docker compose up -d postgres kafka mongodb

# 4. Wait for infrastructure to be ready
Write-Host "`n[2/3] Waiting for infrastructure ports to be open..." -ForegroundColor Cyan

function Wait-For-Port($hostName, $port, $serviceName, $timeoutSec = 45) {
    Write-Host -NoNewline "Checking $serviceName ($hostName`:$port)... "
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    while ($stopwatch.Elapsed.TotalSeconds -lt $timeoutSec) {
        try {
            $tcp = New-Object System.Net.Sockets.TcpClient
            $async = $tcp.BeginConnect($hostName, [int]$port, $null, $null)
            $wait = $async.AsyncWaitHandle.WaitOne(1000)
            if ($wait -and $tcp.Connected) {
                $tcp.EndConnect($async)
                $tcp.Close()
                Write-Host "READY!" -ForegroundColor Green
                return $true
            }
            $tcp.Close()
        } catch {
            # Retry
        }
        Start-Sleep -Seconds 1
    }
    Write-Host "TIMEOUT (Proceeding anyway)" -ForegroundColor Yellow
    return $false
}

Wait-For-Port -hostName $dbHost -port $dbPort -serviceName "PostgreSQL"
Wait-For-Port -hostName "localhost" -port 9092 -serviceName "Kafka"
Wait-For-Port -hostName "localhost" -port 27017 -serviceName "MongoDB"

# 5. Launch each Java Service in its own PowerShell Window
Write-Host "`n[3/3] Launching Java microservices in separate PowerShell windows..." -ForegroundColor Cyan

$services = @(
    @{
        Name = "account-service"
        Port = "8085"
        Dir  = (Join-Path $PSScriptRoot "account-service")
        Cmd  = @"
`$host.UI.RawUI.WindowTitle = 'account-service [Port: 8085]'
`$env:JAVA_TOOL_OPTIONS = '-Duser.timezone=Asia/Kolkata'
`$env:SERVER_PORT = '8085'
`$env:DB_URL = 'jdbc:postgresql://${dbHost}:${dbPort}/account_db'
`$env:DB_USER = '${dbUser}'
`$env:DB_PASSWORD = '${dbPass}'
`$env:KAFKA_BOOTSTRAP_SERVERS = '${kafkaHost}'
Write-Host '=========================================' -ForegroundColor Green
Write-Host '   Account Service (Port 8085)           ' -ForegroundColor Green
Write-Host '=========================================' -ForegroundColor Green
.\mvnw.cmd spring-boot:run
"@
    },
    @{
        Name = "payment-service"
        Port = "8086"
        Dir  = (Join-Path $PSScriptRoot "payment-service")
        Cmd  = @"
`$host.UI.RawUI.WindowTitle = 'payment-service [Port: 8086]'
`$env:JAVA_TOOL_OPTIONS = '-Duser.timezone=Asia/Kolkata'
`$env:SERVER_PORT = '8086'
`$env:DB_URL = 'jdbc:postgresql://${dbHost}:${dbPort}/payment_db'
`$env:DB_USER = '${dbUser}'
`$env:DB_PASSWORD = '${dbPass}'
`$env:KAFKA_BOOTSTRAP_SERVERS = '${kafkaHost}'
Write-Host '=========================================' -ForegroundColor Green
Write-Host '   Payment Service (Port 8086)           ' -ForegroundColor Green
Write-Host '=========================================' -ForegroundColor Green
.\mvnw.cmd spring-boot:run
"@
    },
    @{
        Name = "fraud-service"
        Port = "8087"
        Dir  = (Join-Path $PSScriptRoot "fraud-service")
        Cmd  = @"
`$host.UI.RawUI.WindowTitle = 'fraud-service [Port: 8087]'
`$env:JAVA_TOOL_OPTIONS = '-Duser.timezone=Asia/Kolkata'
`$env:SERVER_PORT = '8087'
`$env:KAFKA_BOOTSTRAP_SERVERS = '${kafkaHost}'
Write-Host '=========================================' -ForegroundColor Green
Write-Host '   Fraud Service (Port 8087)             ' -ForegroundColor Green
Write-Host '=========================================' -ForegroundColor Green
.\mvnw.cmd spring-boot:run
"@
    },
    @{
        Name = "notification-service"
        Port = "8088"
        Dir  = (Join-Path $PSScriptRoot "notification-service")
        Cmd  = @"
`$host.UI.RawUI.WindowTitle = 'notification-service [Port: 8088]'
`$env:JAVA_TOOL_OPTIONS = '-Duser.timezone=Asia/Kolkata'
`$env:SERVER_PORT = '8088'
`$env:KAFKA_BOOTSTRAP_SERVERS = '${kafkaHost}'
`$env:MONGO_URI = '${mongoUri}'
Write-Host '=========================================' -ForegroundColor Green
Write-Host '   Notification & Audit Service (Port 8088)' -ForegroundColor Green
Write-Host '=========================================' -ForegroundColor Green
.\mvnw.cmd spring-boot:run
"@
    },
    @{
        Name = "api-gateway"
        Port = "8090"
        Dir  = (Join-Path $PSScriptRoot "api-gateway")
        Cmd  = @"
`$host.UI.RawUI.WindowTitle = 'api-gateway [Port: 8090]'
`$env:JAVA_TOOL_OPTIONS = '-Duser.timezone=Asia/Kolkata'
`$env:SERVER_PORT = '8090'
`$env:ACCOUNT_SERVICE_URL = 'http://localhost:8085'
`$env:PAYMENT_SERVICE_URL = 'http://localhost:8086'
`$env:JWT_SECRET = '${jwtSecret}'
Write-Host '=========================================' -ForegroundColor Green
Write-Host '   API Gateway (Port 8090)               ' -ForegroundColor Green
Write-Host '=========================================' -ForegroundColor Green
.\mvnw.cmd spring-boot:run
"@
    }
)

foreach ($svc in $services) {
    Write-Host "  -> Launching $($svc.Name) on port $($svc.Port)..." -ForegroundColor White
    Start-Process powershell -WorkingDirectory $svc.Dir -ArgumentList "-NoExit", "-Command", $svc.Cmd
    Start-Sleep -Seconds 2
}

# 6. Print Summary & Health URLs
Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "   All microservices launched!                            " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Health Check URLs (Spring Boot Actuator):" -ForegroundColor Yellow
Write-Host "  - API Gateway:          http://localhost:8090/actuator/health"
Write-Host "  - Account Service:      http://localhost:8085/actuator/health"
Write-Host "  - Payment Service:      http://localhost:8086/actuator/health"
Write-Host "  - Fraud Service:        http://localhost:8087/actuator/health"
Write-Host "  - Notification Service: http://localhost:8088/actuator/health"
Write-Host "  - Frontend UI (Dev):    http://localhost:5173"
Write-Host "`nNext Step: Run the step-by-step PowerShell tests in docs/LOCAL-TESTING.md`n" -ForegroundColor Green
