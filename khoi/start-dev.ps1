$root = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "Starting Cloud E-wallet development environment..." -ForegroundColor Cyan

Write-Host "Starting Docker MySQL..." -ForegroundColor Yellow
Set-Location $root
docker compose up -d

Write-Host "Waiting for MySQL container..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

Write-Host "Starting Spring Boot backend..." -ForegroundColor Green
Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "cd '$root\backend'; cmd /c mvnw.cmd spring-boot:run"
)

Write-Host "Waiting for backend..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

Write-Host "Starting React frontend..." -ForegroundColor Green
Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "cd '$root\frontend'; npm run dev -- --open"
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