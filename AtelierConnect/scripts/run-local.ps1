[CmdletBinding()]
param(
    [ValidateSet('Backend', 'Frontend')][string]$Component = 'Backend',
    [switch]$Build,
    [switch]$InstallDependencies,
    [switch]$Restart,
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

function Test-ProjectBackend {
    param($Process)
    if (-not $Process -or $Process.Name -notmatch '^javaw?\.exe$' -or -not $Process.CommandLine) { return $false }
    $commandLine = $Process.CommandLine.Replace('/', '\')
    $projectTarget = [IO.Path]::GetFullPath((Join-Path $root 'target')) + '\'
    if ($commandLine.IndexOf($projectTarget, [StringComparison]::OrdinalIgnoreCase) -lt 0) { return $false }
    return $commandLine.Contains('com.atelierconnect.AtelierConnectApplication') -or
        $commandLine.Contains('atelierconnect-backend-0.0.1-SNAPSHOT.jar')
}

try {
    if ($Component -eq 'Frontend') {
        if ($Restart) { throw '-Restart applies only to the backend. Stop Angular with Ctrl+C before starting it again.' }
        Set-Location $frontend
        Get-Command npm.cmd -ErrorAction Stop | Out-Null
        if (-not (Test-Path -LiteralPath (Join-Path $frontend 'package.json'))) {
            throw "Frontend package.json missing in $frontend"
        }
        if (Get-NetTCPConnection -LocalPort 4200 -State Listen -ErrorAction SilentlyContinue) {
            throw 'Frontend port 4200 is already occupied. Stop the existing Angular server with Ctrl+C before restarting.'
        }

        $dependencyFiles = @('node_modules/@angular/cli/bin/ng.js',
            'node_modules/@angular/build/package.json', 'node_modules/typescript/lib/tsc.js',
            'node_modules/@esbuild/win32-x64/esbuild.exe')
        $needsInstall = $InstallDependencies.IsPresent
        foreach ($file in $dependencyFiles) {
            if (-not (Test-Path -LiteralPath (Join-Path $frontend $file))) { $needsInstall = $true }
        }
        $lockFile = Join-Path $frontend 'package-lock.json'
        if (-not (Test-Path -LiteralPath $lockFile)) { throw 'Frontend package-lock.json missing; npm ci requires the tracked lockfile.' }
        $lockHash = (Get-FileHash -LiteralPath $lockFile -Algorithm SHA256).Hash
        $dependencyStamp = Join-Path $root '.local/diagnostics/frontend-dependencies.sha256'
        if (Test-Path -LiteralPath $dependencyStamp) {
            if ((Get-Content -LiteralPath $dependencyStamp -Raw).Trim() -ne $lockHash) { $needsInstall = $true }
        }

        if ($needsInstall) {
            # Only inspect this workspace's esbuild image. Never stop other Node applications.
            $esbuildPath = [IO.Path]::GetFullPath((Join-Path $frontend 'node_modules/@esbuild/win32-x64/esbuild.exe'))
            $workers = @(Get-CimInstance Win32_Process -Filter "Name = 'esbuild.exe'" | Where-Object {
                $_.ExecutablePath -and [string]::Equals($_.ExecutablePath, $esbuildPath, [StringComparison]::OrdinalIgnoreCase)
            })
            if ($workers.Count) {
                $workerIds = ($workers | ForEach-Object ProcessId) -join ', '
                throw "Frontend dependencies are in use by this project's esbuild (PID: $workerIds). Stop Angular/watch tasks with Ctrl+C before installing. npm ci was not executed."
            }
            Write-Host 'Installing frontend dependencies with npm ci...'
            try { Invoke-Native 'npm.cmd' @('ci') } catch {
                throw "Frontend dependency installation failed: $($_.Exception.Message). If npm reports EPERM on esbuild.exe, stop this project's Angular/watch processes and retry. Run manual npm commands from $frontend."
            }
            New-Item -ItemType Directory -Path (Split-Path $dependencyStamp -Parent) -Force | Out-Null
            Set-Content -LiteralPath $dependencyStamp -Value $lockHash -Encoding ASCII
        } else {
            Write-Host 'Reusing installed frontend dependencies. Use -InstallDependencies after dependency changes or to repair an installation.'
        }
        if ($Build) {
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
    $backendToRestart = $null
    $listeners = @(Get-NetTCPConnection -LocalPort 8082 -State Listen -ErrorAction SilentlyContinue)
    if ($listeners.Count) {
        $listenerIds = @($listeners.OwningProcess | Select-Object -Unique)
        if ($listenerIds.Count -ne 1) { throw 'Port 8082 has multiple owners. No process was stopped.' }
        $backendToRestart = Get-CimInstance Win32_Process -Filter ('ProcessId=' + $listenerIds[0])
        if (-not (Test-ProjectBackend $backendToRestart)) {
            throw "Port 8082 is occupied by an unrecognized process (PID $($listenerIds[0])). No process was stopped."
        }
        if (-not $Restart) {
            throw "AtelierConnect is already listening on 8082 (PID $($listenerIds[0])). To compile and restart it, use: .\scripts\run-local.ps1 -Component Backend -Build -Restart. To compile only, use: .\mvnw.cmd -DskipTests package."
        }
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

    if ($backendToRestart) {
        # Verify again after the credential checks; never stop a recycled PID or unrelated service.
        $currentBackend = Get-CimInstance Win32_Process -Filter ('ProcessId=' + $backendToRestart.ProcessId)
        if ($currentBackend) {
            if ($currentBackend.CreationDate -ne $backendToRestart.CreationDate -or -not (Test-ProjectBackend $currentBackend)) {
                throw 'Backend process identity changed. No process was stopped.'
            }
            Write-Host "Restarting this project's backend (PID $($currentBackend.ProcessId))..."
            Stop-Process -Id $currentBackend.ProcessId -ErrorAction Stop
        }
        $portDeadline = (Get-Date).AddSeconds(15)
        while (Get-NetTCPConnection -LocalPort 8082 -State Listen -ErrorAction SilentlyContinue) {
            if ((Get-Date) -ge $portDeadline) { throw 'Port 8082 was not released. Inspect its current owner.' }
            Start-Sleep -Milliseconds 500
        }
    }

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
