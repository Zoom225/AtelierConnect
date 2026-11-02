[CmdletBinding()]
param(
    [ValidateSet('Backend', 'Frontend')][string]$Component = 'Backend',
    [switch]$Build,
    [switch]$NonInteractive
)

# Launcher local. No passwords, JWT secrets or database data are written to Git.
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$frontend = Join-Path $root 'frontend'
$original = Get-Location

function Invoke-Native {
    param([string]$Executable, [string[]]$Arguments)
    # PowerShell 5.1 can treat harmless warnings from native stderr as errors.
    $ErrorActionPreference = 'Continue'
    & $Executable @Arguments
    $code = $LASTEXITCODE
    if ($code -ne 0) {
        throw "$Executable exited with code $code"
    }
}

try {
    if ($Component -eq 'Frontend') {
        Set-Location $frontend
        Get-Command npm.cmd -ErrorAction Stop | Out-Null
        if ($Build) {
            Invoke-Native 'npm.cmd' @('ci')
            Invoke-Native 'npm.cmd' @('run', 'build')
        }
        Write-Host 'Starting Angular at http://localhost:4200 (API proxy -> :8082)'
        Invoke-Native 'npm.cmd' @('start')
        return
    }

    Set-Location $root
    foreach ($command in @('java', 'docker', 'curl.exe')) {
        Get-Command $command -ErrorAction Stop | Out-Null
    }
    Invoke-Native 'docker' @('info', '--format', '{{.ServerVersion}}')
    if (Get-NetTCPConnection -LocalPort 8082 -State Listen -ErrorAction SilentlyContinue) {
        throw 'Port 8082 is already occupied. Inspect the existing process before restarting it.'
    }
    Get-Command docker -ErrorAction Stop | Out-Null
    if (-not (Test-Path (Join-Path $root 'mvnw.cmd'))) {
        throw 'Maven wrapper not found in backend directory.'
    }

    # Required values must be available to Java, not just to a different terminal.
    & (Join-Path $PSScriptRoot 'set-local-environment.ps1') -NonInteractive:$NonInteractive
    if ($env:POSTGRES_USER -ne 'atelierconnect') {
        Write-Warning 'POSTGRES_USER differs from atelierconnect; preserving the existing value.'
    }

    # Reuse any existing application container, even if created by another checkout.
    $ErrorActionPreference = 'Continue'
    $running = & docker container inspect 'atelierconnect-db' --format '{{.State.Running}}' 2>$null
    $found = ($LASTEXITCODE -eq 0)
    $ErrorActionPreference = 'Stop'
    if ($found) {
        # Preserve existing demo accounts; keep the configured Hibernate update mode.
        $env:APP_DATA_INITIALIZE = 'false'
        if (($running | Out-String).Trim() -ne 'true') {
            Invoke-Native 'docker' @('start', 'atelierconnect-db')
        }
    } else {
        # Refuse to silently initialize an empty application database.
        Invoke-Native 'docker' @('volume', 'inspect', 'atelierconnect_atelierconnect-postgres-data', '--format', '{{.Name}}')
        $env:APP_DATA_INITIALIZE = 'false'
        Invoke-Native 'docker' @('compose', 'up', '-d', '--no-recreate', 'postgres')
    }

    $env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'update'
    $deadline = (Get-Date).AddSeconds(60)
    do {
        & docker exec atelierconnect-db pg_isready -q -d atelierconnect_db
        if ($LASTEXITCODE -eq 0) { break }
        if ((Get-Date) -ge $deadline) { throw 'PostgreSQL readiness timed out.' }
        Start-Sleep -Seconds 1
    } while ($true)

    Invoke-Native 'docker' @('compose', 'config', '--quiet')
    Write-Host 'PostgreSQL container available. Existing volume preserved.'

    # A healthy container alone does not prove JDBC credentials work.
    # Ask for the existing role password before attempting Hibernate startup.
    & (Join-Path $PSScriptRoot 'verify-postgres-login.ps1') -NonInteractive:$NonInteractive

    $jar = Join-Path $root 'target/atelierconnect-backend-0.0.1-SNAPSHOT.jar'
    $needsBuild = -not (Test-Path -LiteralPath $jar)
    if (-not $needsBuild) {
        $jarDate = (Get-Item -LiteralPath $jar).LastWriteTimeUtc
        $needsBuild = @(@(Get-Item (Join-Path $root 'pom.xml')) + @(Get-ChildItem (Join-Path $root 'src/main') -Recurse -File) | Where-Object LastWriteTimeUtc -gt $jarDate).Count -gt 0
    }
    if ($Build -or $needsBuild) {
        # Never run integration tests against the existing application database.
        Invoke-Native (Join-Path $root 'mvnw.cmd') @('-DskipTests', 'package')
    }

    # With DevTools restart enabled, a failed application startup may still
    # produce Maven BUILD SUCCESS. Disable only the restart classloader here.
    $env:SPRING_DEVTOOLS_RESTART_ENABLED = 'false'
    Write-Host 'Starting Spring Boot at http://localhost:8082 (Swagger: /swagger-ui.html)'
    if (-not (Test-Path -LiteralPath $jar)) { throw 'Packaged backend JAR is missing.' }
    $report = Join-Path $root '.local/diagnostics'
    New-Item -ItemType Directory -Path $report -Force | Out-Null
    $log = Join-Path $report ('startup-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff') + '.log')
    $shell = (Get-Process -Id $PID).Path
    $worker = Join-Path $PSScriptRoot 'start-backend-logged.ps1'
    $launcher = Start-Process -FilePath $shell -WindowStyle Hidden -PassThru -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"' + $worker + '"'), '-StartupLog', ('"' + $log + '"'))
    try {
        & (Join-Path $PSScriptRoot 'verify-backend.ps1') -StartupLog $log -TimeoutSeconds 180
    } catch {
        Write-Host "Startup failed. Sanitized diagnostic log: $log"
        if (Test-Path -LiteralPath $log) { Get-Content -LiteralPath $log -Tail 60 | Write-Host }
        throw
    }
    Write-Host "Backend verified. Launcher PID=$($launcher.Id); log: $log"
}
finally {
    Set-Location $original
}
