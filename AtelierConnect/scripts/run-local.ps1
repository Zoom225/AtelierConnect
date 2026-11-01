[CmdletBinding()]
param(
    [ValidateSet('Backend', 'Frontend')][string]$Component = 'Backend',
    [switch]$Build
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
    Get-Command docker -ErrorAction Stop | Out-Null
    if (-not (Test-Path (Join-Path $root 'mvnw.cmd'))) {
        throw 'Maven wrapper not found in backend directory.'
    }

    # Required values must be available to Java, not just to a different terminal.
    & (Join-Path $PSScriptRoot 'set-local-environment.ps1')
    if ($env:POSTGRES_USER -ne 'atelierconnect') {
        Write-Warning 'POSTGRES_USER differs from atelierconnect; preserving the existing value.'
    }

    # Reuse any existing application container, even if created by another checkout.
    $ErrorActionPreference = 'Continue'
    $running = & docker container inspect 'atelierconnect-db' --format '{{.State.Running}}' 2>$null
    $found = ($LASTEXITCODE -eq 0)
    $ErrorActionPreference = 'Stop'
    if ($found) {
        if (($running | Out-String).Trim() -ne 'true') {
            Invoke-Native 'docker' @('start', 'atelierconnect-db')
        }
    } else {
        Invoke-Native 'docker' @('compose', 'up', '-d', '--no-recreate', 'postgres')
    }

    Invoke-Native 'docker' @('compose', 'config', '--quiet')
    Write-Host 'PostgreSQL container available. Existing volume preserved.'

    # A healthy container alone does not prove JDBC credentials work.
    # Ask for the existing role password before attempting Hibernate startup.
    & (Join-Path $PSScriptRoot 'verify-postgres-login.ps1')

    if ($Build) {
        # Never run integration tests against the existing application database.
        Invoke-Native (Join-Path $root 'mvnw.cmd') @('-DskipTests', 'package')
    }

    # With DevTools restart enabled, a failed application startup may still
    # produce Maven BUILD SUCCESS. Disable only the restart classloader here.
    $env:SPRING_DEVTOOLS_RESTART_ENABLED = 'false'
    Write-Host 'Starting Spring Boot at http://localhost:8082 (Swagger: /swagger-ui.html)'
    Invoke-Native (Join-Path $root 'mvnw.cmd') @('spring-boot:run')
}
finally {
    Set-Location $original
}
