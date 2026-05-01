<#
.SYNOPSIS
    Inicia todos os microsserviços do FinPred e o frontend simultaneamente.
.DESCRIPTION
    Script para desenvolvimento local. Sobe os 4 microsserviços Java com perfil 'dev'
    (H2 em memória) e o frontend Vite em paralelo.
.NOTES
    Uso: .\start_services.ps1
    Para parar: feche a janela ou pressione Ctrl+C
#>

$ErrorActionPreference = "Continue"
$ProjectRoot = $PSScriptRoot

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  FinPred - Iniciando Servicos" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Set JAVA_HOME explicitly
$env:JAVA_HOME = "C:\Program Files\Java\jdk-26"

# Verificar Java
try {
    $javaVersion = java --version 2>&1 | Select-Object -First 1
    Write-Host "[OK] Java: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "[ERRO] Java nao encontrado no PATH!" -ForegroundColor Red
    exit 1
}

# Verificar Node
try {
    $nodeVersion = node --version 2>&1
    Write-Host "[OK] Node: $nodeVersion" -ForegroundColor Green
} catch {
    Write-Host "[ERRO] Node.js nao encontrado no PATH!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Iniciando microsservicos..." -ForegroundColor Yellow
Write-Host ""

# Definir servicos
$services = @(
    @{ Name = "API Gateway";        Port = 8080; Path = "backend\api-gateway" },
    @{ Name = "Auth Service";       Port = 8081; Path = "backend\auth-service" },
    @{ Name = "Core Service";       Port = 8082; Path = "backend\core-service" },
    @{ Name = "Prediction Service"; Port = 8083; Path = "backend\prediction-service" }
)

$jobs = @()

# Iniciar cada microsservico em background
foreach ($svc in $services) {
    $svcPath = Join-Path $ProjectRoot $svc.Path
    $mvnwPath = Join-Path $svcPath "mvnw.cmd"

    Write-Host "  Iniciando $($svc.Name) na porta $($svc.Port)..." -ForegroundColor Cyan

    $job = Start-Process -FilePath $mvnwPath `
        -ArgumentList "spring-boot:run", "-Dspring-boot.run.profiles=dev" `
        -WorkingDirectory $svcPath `
        -PassThru `
        -WindowStyle Normal

    $jobs += @{ Name = $svc.Name; Process = $job; Port = $svc.Port }
}

# Iniciar frontend
Write-Host "  Iniciando Frontend (Vite)..." -ForegroundColor Cyan
$frontendPath = Join-Path $ProjectRoot "frontend"
$frontendJob = Start-Process -FilePath "npm.cmd" `
    -ArgumentList "run", "dev" `
    -WorkingDirectory $frontendPath `
    -PassThru `
    -WindowStyle Normal

$jobs += @{ Name = "Frontend"; Process = $frontendJob; Port = 5173 }

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Todos os servicos iniciados!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Endpoints:" -ForegroundColor White
Write-Host "    Frontend:           http://localhost:5173" -ForegroundColor Gray
Write-Host "    API Gateway:        http://localhost:8080" -ForegroundColor Gray
Write-Host "    Auth Service:       http://localhost:8081" -ForegroundColor Gray
Write-Host "    Core Service:       http://localhost:8082" -ForegroundColor Gray
Write-Host "    Prediction Service: http://localhost:8083" -ForegroundColor Gray
Write-Host ""
Write-Host "  Swagger UI:" -ForegroundColor White
Write-Host "    Auth:       http://localhost:8081/swagger-ui.html" -ForegroundColor Gray
Write-Host "    Core:       http://localhost:8082/swagger-ui.html" -ForegroundColor Gray
Write-Host "    Prediction: http://localhost:8083/swagger-ui.html" -ForegroundColor Gray
Write-Host ""
Write-Host "  H2 Console:" -ForegroundColor White
Write-Host "    Auth DB:       http://localhost:8081/h2-console" -ForegroundColor Gray
Write-Host "    Core DB:       http://localhost:8082/h2-console" -ForegroundColor Gray
Write-Host "    Prediction DB: http://localhost:8083/h2-console" -ForegroundColor Gray
Write-Host ""
Write-Host "Pressione Enter para encerrar todos os servicos..." -ForegroundColor Yellow
Read-Host

# Encerrar todos os processos
Write-Host ""
Write-Host "Encerrando servicos..." -ForegroundColor Yellow
foreach ($job in $jobs) {
    try {
        if (-not $job.Process.HasExited) {
            Stop-Process -Id $job.Process.Id -Force -ErrorAction SilentlyContinue
            # Também mata processos filhos (Java/Node)
            Get-Process | Where-Object { $_.Parent.Id -eq $job.Process.Id } | Stop-Process -Force -ErrorAction SilentlyContinue
            Write-Host "  [x] $($job.Name) encerrado" -ForegroundColor Red
        }
    } catch {
        Write-Host "  [!] Erro ao encerrar $($job.Name)" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Todos os servicos foram encerrados." -ForegroundColor Green
