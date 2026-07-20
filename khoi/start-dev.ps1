$root = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "Starting Cloud E-wallet development environment..." -ForegroundColor Cyan

# ==============================
# Load local environment variables
# ==============================
$envFile = Join-Path $root ".env.local"

if (-not (Test-Path $envFile)) {
    Write-Error "Missing .env.local file at: $envFile"
    Write-Host "Create .env.local with:" -ForegroundColor Yellow
    Write-Host "JWT_SECRET=your-local-secret-at-least-32-bytes"
    Write-Host "JWT_EXPIRATION=3600"
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()

    if ($line -and -not $line.StartsWith("#")) {
        $parts = $line -split "=", 2

        if ($parts.Count -eq 2) {
            $name = $parts[0].Trim()
            $value = $parts[1].Trim()

            [Environment]::SetEnvironmentVariable(
                $name,
                $value,
                "Process"
            )
        }
    }
}

if (-not $env:JWT_SECRET) {
    Write-Error "JWT_SECRET is missing in .env.local"
    exit 1
}

if (-not $env:JWT_EXPIRATION) {
    $env:JWT_EXPIRATION = "3600"
}

Write-Host "Local environment variables loaded." -ForegroundColor Green

# ==============================
# Start Docker MySQL
# ==============================
Write-Host "Starting Docker MySQL..." -ForegroundColor Yellow
Set-Location $root
docker compose up -d

if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to start Docker services."
    exit 1
}

Write-Host "Waiting for MySQL container..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# ==============================
# Start Spring Boot backend
# ==============================
Write-Host "Starting Spring Boot backend..." -ForegroundColor Green

$backendCommand = @"
`$env:JWT_SECRET='$($env:JWT_SECRET)'
`$env:JWT_EXPIRATION='$($env:JWT_EXPIRATION)'
Set-Location '$root\backend'
cmd /c mvnw.cmd spring-boot:run
"@

Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    $backendCommand
)

Write-Host "Waiting for backend..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# ==============================
# Start React frontend
# ==============================
Write-Host "Starting React frontend..." -ForegroundColor Green

Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "Set-Location '$root\frontend'; npm run dev -- --open"
)

Write-Host "Done." -ForegroundColor Cyan
Write-Host "Backend:  http://localhost:8080"
Write-Host "Frontend: http://localhost:5173"













# $root = Split-Path -Parent $MyInvocation.MyCommand.Path

# Write-Host "Starting Cloud E-wallet development environment..." -ForegroundColor Cyan

# Write-Host "Checking Docker..." -ForegroundColor Yellow
# docker info > $null 2>&1

# if ($LASTEXITCODE -ne 0) {
#     Write-Host "Docker is not running. Please open Docker Desktop first." -ForegroundColor Red
#     exit 1
# }

# Write-Host "Starting Docker MySQL..." -ForegroundColor Yellow
# Set-Location $root
# docker compose up -d

# if ($LASTEXITCODE -ne 0) {
#     Write-Host "Docker Compose failed. Backend/frontend will not start." -ForegroundColor Red
#     Write-Host "Possible reasons:" -ForegroundColor Yellow
#     Write-Host "- Docker Desktop is not fully started"
#     Write-Host "- Container name conflict, for example ewallet_mysql already exists"
#     Write-Host ""
#     Write-Host "Check containers with:" -ForegroundColor Cyan
#     Write-Host "docker ps -a"
#     Write-Host ""
#     Write-Host "If you want to keep database data, remove only the old container, not volume:" -ForegroundColor Cyan
#     Write-Host "docker rm ewallet_mysql"
#     Write-Host "docker compose up -d"
#     exit 1
# }

# Write-Host "Waiting for MySQL container..." -ForegroundColor Yellow
# Start-Sleep -Seconds 5

# Write-Host "Checking MySQL container..." -ForegroundColor Yellow
# docker ps --filter "name=ewallet_mysql"

# Write-Host "Starting Spring Boot backend..." -ForegroundColor Green
# Start-Process powershell -ArgumentList @(
#     "-NoExit",
#     "-Command",
#     "cd '$root\backend'; cmd /c mvnw.cmd spring-boot:run"
# )

# Write-Host "Waiting for backend..." -ForegroundColor Yellow
# Start-Sleep -Seconds 5

# Write-Host "Starting React frontend..." -ForegroundColor Green
# Start-Process powershell -ArgumentList @(
#     "-NoExit",
#     "-Command",
#     "cd '$root\frontend'; npm run dev -- --open"
# )

# Write-Host "Done." -ForegroundColor Cyan
# Write-Host "Backend:  http://localhost:8080"
# Write-Host "Frontend: http://localhost:5173"