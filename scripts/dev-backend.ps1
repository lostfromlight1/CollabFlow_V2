# Starts or stops the CollabFlow backend as one stack.
# Each Spring Boot app is its own JVM (own port). This script is the supervisor:
# it starts Docker infra, then each app, and records process ids so stop can
# kill the whole process tree (gradlew/cmd -> java, or go run -> compiled binary).
#
#   scripts\dev-backend.cmd start
#   scripts\dev-backend.cmd status
#   scripts\dev-backend.cmd stop

param(
    [Parameter(Position = 0)]
    [ValidateSet("start", "stop", "status")]
    [string]$Action = "start"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$JavaDir = Join-Path $Root "backend\java-services"
$GoRoot = Join-Path $Root "backend\go-services"
$ComposeDir = Join-Path $Root "backend\infrastructure"
$EnvFile = Join-Path $Root ".env"
$LogDir = Join-Path $Root "logs\backend"
$PidFile = Join-Path $LogDir "pids.json"

$Services = @(
    @{ Name = "auth";     Kind = "java"; Module = ":auth-service:bootRun";     Health = "http://localhost:8081/api/v1/auth/health" }
    @{ Name = "platform"; Kind = "java"; Module = ":platform-service:bootRun"; Health = "http://localhost:8082/api/v1/platform/health" }
    @{ Name = "user";     Kind = "java"; Module = ":user-service:bootRun";     Health = "http://localhost:8084/api/v1/users/health" }
    @{ Name = "task";     Kind = "java"; Module = ":task-service:bootRun";     Health = "http://localhost:8083/api/v1/tasks/health" }
    @{ Name = "gateway";  Kind = "java"; Module = ":api-gateway:bootRun";      Health = "http://localhost:8080/actuator/health" }
    @{ Name = "collab";   Kind = "go";   Dir = "collab-service";               Health = "http://localhost:9081/api/v1/collab/health" }
    @{ Name = "realtime"; Kind = "go";   Dir = "realtime-service";             Health = "http://localhost:9082/api/v1/realtime/health" }
    @{ Name = "email";    Kind = "go";   Dir = "email-service";                Health = "http://localhost:9084/api/v1/email/health" }
    @{ Name = "document"; Kind = "go";   Dir = "document-service";             Health = "http://localhost:9083/api/v1/documents/health" }
)

function Read-PidMap {
    $map = @{}
    if (-not (Test-Path $PidFile)) { return $map }
    $raw = Get-Content $PidFile -Raw
    if ([string]::IsNullOrWhiteSpace($raw)) { return $map }
    $obj = $raw | ConvertFrom-Json
    foreach ($prop in $obj.PSObject.Properties) {
        $map[$prop.Name] = [int]$prop.Value
    }
    return $map
}

function Write-PidMap($map) {
    New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
    $map | ConvertTo-Json | Set-Content -Path $PidFile -Encoding utf8
}

function Test-Alive([int]$ProcessId) {
    if ($ProcessId -le 0) { return $false }
    return $null -ne (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)
}

function Import-DotEnv {
    if (-not (Test-Path $EnvFile)) {
        throw "Missing $EnvFile. Copy .env.example to .env at the repo root."
    }
    Get-Content $EnvFile | ForEach-Object {
        if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
        $k, $v = $_ -split '=', 2
        $name = $k.Trim()
        if (-not $name) { return }
        Set-Item -Path "Env:$name" -Value $v.Trim()
    }
}

function Start-Infra {
    Write-Host "Starting Docker infra (postgres, redis, kafka, mailhog)..."
    Push-Location $ComposeDir
    try {
        docker compose --env-file $EnvFile up -d
    } finally {
        Pop-Location
    }
}

function Start-One($svc, $map) {
    $existing = 0
    if ($map.ContainsKey($svc.Name)) { $existing = [int]$map[$svc.Name] }
    if (Test-Alive $existing) {
        Write-Host "  $($svc.Name) already running (pid $existing)"
        return
    }

    $out = Join-Path $LogDir "$($svc.Name).log"
    $err = Join-Path $LogDir "$($svc.Name).err.log"
    if ($svc.Kind -eq "java") {
        # cmd /c keeps a parent we can kill with the Java child (taskkill /T).
        $proc = Start-Process -FilePath "cmd.exe" `
            -ArgumentList @("/c", "gradlew.bat $($svc.Module)") `
            -WorkingDirectory $JavaDir `
            -RedirectStandardOutput $out `
            -RedirectStandardError $err `
            -WindowStyle Hidden `
            -PassThru
    } else {
        $dir = Join-Path $GoRoot $svc.Dir
        $proc = Start-Process -FilePath "go" `
            -ArgumentList @("run", "./cmd/server") `
            -WorkingDirectory $dir `
            -RedirectStandardOutput $out `
            -RedirectStandardError $err `
            -WindowStyle Hidden `
            -PassThru
    }
    $map[$svc.Name] = $proc.Id
    Write-Host "  $($svc.Name) started (pid $($proc.Id)) -> $out"
}

function Stop-One([string]$Name, [int]$ProcessId) {
    if (-not (Test-Alive $ProcessId)) {
        Write-Host "  $Name not running"
        return
    }
    & taskkill.exe /PID $ProcessId /T /F | Out-Null
    Write-Host "  $Name stopped (pid $ProcessId)"
}

function Show-Status($map) {
    foreach ($svc in $Services) {
        $processId = 0
        if ($map.ContainsKey($svc.Name)) { $processId = [int]$map[$svc.Name] }
        $alive = Test-Alive $processId
        $health = "down"
        if ($alive) {
            try {
                $resp = Invoke-WebRequest -Uri $svc.Health -UseBasicParsing -TimeoutSec 2
                if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 300) { $health = "up" }
            } catch {
                $health = "starting"
            }
        }
        "{0,-10} {1,-10} pid={2}" -f $svc.Name, $health, $(if ($alive) { $processId } else { "-" })
    }
    Write-Host "Logs: $LogDir"
}

switch ($Action) {
    "start" {
        Import-DotEnv
        Start-Infra
        New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
        $map = Read-PidMap
        Write-Host "Starting apps (first boot compiles; logs under logs/backend)..."
        foreach ($svc in $Services) {
            Start-One $svc $map
        }
        Write-PidMap $map
        Write-Host "Use 'scripts\dev-backend.cmd status' after a minute. Gateway is :8080."
    }
    "stop" {
        $map = Read-PidMap
        foreach ($svc in $Services) {
            $processId = 0
            if ($map.ContainsKey($svc.Name)) { $processId = [int]$map[$svc.Name] }
            Stop-One $svc.Name $processId
        }
        Write-PidMap @{}
        Write-Host "Apps stopped. Docker infra is still up (data kept). Stop it with:"
        Write-Host "  cd backend\infrastructure; docker compose --env-file ..\..\.env stop"
    }
    "status" {
        Show-Status (Read-PidMap)
    }
}
