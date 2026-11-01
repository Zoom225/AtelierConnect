[CmdletBinding()]
param(
    [string]$TestContainer = 'atelierconnect-validation-db',
    [ValidateRange(1024, 65535)][int]$PostgresPort = 5549,
    [switch]$SkipInstall
)

# Windows PowerShell 5.1 and PowerShell 7. No application database is used.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$runId = Get-Date -Format 'yyyyMMdd_HHmmss_fff'
$reportDir = Join-Path $projectRoot ".local/validation/$runId"
New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
$originalLocation = Get-Location
$runtimeVariables = @('POSTGRES_USER', 'POSTGRES_PASSWORD', 'SPRING_DATASOURCE_URL',
    'JWT_SECRET', 'APP_DEMO_MEMBER_PASSWORD', 'APP_DEMO_ADMIN_PASSWORD',
    'RUN_POSTGRES_CONCURRENCY_TEST', 'PLAYWRIGHT_JSON_OUTPUT_NAME', 'POSTGRES_DB')
$savedEnvironment = @{}
foreach ($name in $runtimeVariables) { $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name) }
$backend = $null
$frontend = $null

function New-RandomSecret {
    $bytes = New-Object byte[] 48
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes)
}

function Invoke-Checked {
    param([string]$Executable, [string[]]$Arguments, [string]$LogName)
    # Windows PowerShell treats native stderr warnings as ErrorRecords. Only the
    # actual exit code decides success, and full output stays in the local log.
    $ErrorActionPreference = 'Continue'
    & $Executable @Arguments *> (Join-Path $reportDir $LogName)
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) { throw "Command failed (exit $exitCode). See $reportDir/$LogName" }
}

function Invoke-TestSql {
    param([string]$Database, [string]$Sql)
    if ($Database -notmatch '^atelierconnect_(test|browser)_\d{8}_\d{6}_\d{3}$') {
        throw 'Refusing SQL outside the databases created by this validation run.'
    }
    $value = & docker exec $TestContainer psql -X -v ON_ERROR_STOP=1 -U $env:POSTGRES_USER -d $Database -tAc $Sql
    if ($LASTEXITCODE -ne 0) { throw 'Test database query failed.' }
    return ($value -join "`n").Trim()
}

function Wait-Http {
    param([string]$Url, $Process)
    $deadline = (Get-Date).AddMinutes(3)
    while ((Get-Date) -lt $deadline) {
        if ($Process.HasExited) { throw "Application stopped before readiness. See $reportDir" }
        try {
            $response = Invoke-WebRequest $Url -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200) { return }
        } catch { }
        Start-Sleep -Seconds 2
    }
    throw "Readiness timeout for $Url. See $reportDir"
}

function Stop-OwnedProcesses {
    foreach ($process in @($frontend, $backend)) {
        if ($null -ne $process -and -not $process.HasExited) {
            # Both PIDs were created by this script; never stop a pre-existing server.
            & taskkill.exe /PID $process.Id /T /F *> $null
        }
    }
}

try {
    Set-Location $projectRoot
    foreach ($command in @('java', 'node', 'npm.cmd', 'docker')) {
        Get-Command $command -ErrorAction Stop | Out-Null
    }
    if (Get-NetTCPConnection -LocalPort 8082, 4200 -State Listen -ErrorAction SilentlyContinue) {
        throw 'Ports 8082/4200 are in use. Existing servers were not stopped.'
    }
    $nodeVersion = & node --version
    Write-Host "Validation uses $nodeVersion; application ports remain 8082 and 4200."

    $ErrorActionPreference = 'Continue'
    $inspection = & docker inspect $TestContainer 2>$null
    $containerExists = $LASTEXITCODE -eq 0
    $ErrorActionPreference = 'Stop'
    if (-not $containerExists) {
        if (Get-NetTCPConnection -LocalPort $PostgresPort -State Listen -ErrorAction SilentlyContinue) {
            throw "Test PostgreSQL port $PostgresPort is already occupied."
        }
        $env:POSTGRES_USER = 'atelier_validation'
        $env:POSTGRES_PASSWORD = New-RandomSecret
        $env:POSTGRES_DB = 'atelierconnect_validation'
        try {
            Invoke-Checked docker @('run', '-d', '--name', $TestContainer,
                '--label', 'atelierconnect.validation=true', '-p', "127.0.0.1:${PostgresPort}:5432",
                '--mount', "type=volume,source=$TestContainer-data,target=/var/lib/postgresql/data",
                '-e', 'POSTGRES_USER', '-e', 'POSTGRES_PASSWORD', '-e', 'POSTGRES_DB', 'postgres:15') 'postgres-start.log'
        } finally { Remove-Item Env:POSTGRES_DB -ErrorAction SilentlyContinue }
    } else {
        $metadata = ($inspection | ConvertFrom-Json)[0]
        $initialDatabase = ($metadata.Config.Env | Where-Object { $_ -like 'POSTGRES_DB=*' }) -replace '^POSTGRES_DB=', ''
        if ($initialDatabase -notmatch '^atelierconnect_.*(test|validation)' -or $metadata.Config.Image -ne 'postgres:15') {
            throw 'Existing container is not a dedicated AtelierConnect PostgreSQL 15 test server.'
        }
        $env:POSTGRES_USER = ($metadata.Config.Env | Where-Object { $_ -like 'POSTGRES_USER=*' }) -replace '^POSTGRES_USER=', ''
        $env:POSTGRES_PASSWORD = ($metadata.Config.Env | Where-Object { $_ -like 'POSTGRES_PASSWORD=*' }) -replace '^POSTGRES_PASSWORD=', ''
        Invoke-Checked docker @('start', $TestContainer) 'postgres-start.log'
    }
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        $ErrorActionPreference = 'Continue'
        & docker exec $TestContainer pg_isready -U $env:POSTGRES_USER *> $null
        $ready = $LASTEXITCODE -eq 0
        $ErrorActionPreference = 'Stop'
        if ($ready) { break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'Test PostgreSQL did not become ready.' }
    $metadata = (docker inspect $TestContainer | ConvertFrom-Json)[0]
    if (@($metadata.NetworkSettings.Ports.'5432/tcp'.HostPort) -notcontains "$PostgresPort") {
        throw 'Test container port does not match PostgresPort. No database was modified.'
    }

    $testDatabase = "atelierconnect_test_$runId"
    $browserDatabase = "atelierconnect_browser_$runId"
    foreach ($database in @($testDatabase, $browserDatabase)) {
        Invoke-Checked docker @('exec', $TestContainer, 'createdb', '-U', $env:POSTGRES_USER, $database) "$database-create.log"
    }
    $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://127.0.0.1:$PostgresPort/$testDatabase"
    $env:JWT_SECRET = New-RandomSecret
    $env:APP_DEMO_MEMBER_PASSWORD = New-RandomSecret
    $env:APP_DEMO_ADMIN_PASSWORD = New-RandomSecret
    $env:RUN_POSTGRES_CONCURRENCY_TEST = 'true'
    Write-Host 'Compiling and testing Java against the dedicated test database...'
    Invoke-Checked (Join-Path $projectRoot 'mvnw.cmd') @('-B', '-ntp', 'clean', 'verify') 'maven.log'

    Set-Location (Join-Path $projectRoot 'frontend')
    if (-not $SkipInstall) { Invoke-Checked npm.cmd @('ci') 'npm-ci.log' }
    Invoke-Checked npm.cmd @('test', '--', '--maxWorkers=2', '--reporter=default', '--reporter=json',
        "--outputFile=$(Join-Path $reportDir 'vitest.json')") 'vitest.log'
    Invoke-Checked npm.cmd @('run', 'build') 'angular-build.log'
    Set-Location (Join-Path $projectRoot 'frontend/e2e')
    if (-not $SkipInstall) { Invoke-Checked npm.cmd @('ci', '--no-audit', '--no-fund') 'playwright-ci.log' }
    Invoke-Checked node @('node_modules/@playwright/test/cli.js', 'install', 'chromium') 'chromium-install.log'

    # Resolve the real JDK executable: Oracle javapath can spawn a different PID.
    $ErrorActionPreference = 'Continue'
    $javaSettings = (& java -XshowSettings:properties -version 2>&1 | Out-String)
    $ErrorActionPreference = 'Stop'
    $javaHome = [regex]::Match($javaSettings, '(?m)^\s*java.home\s*=\s*(.+)$').Groups[1].Value.Trim()
    if (-not $javaHome) { throw 'Cannot resolve the JDK home.' }
    $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://127.0.0.1:$PostgresPort/$browserDatabase"
    $jar = Join-Path $projectRoot 'target/atelierconnect-backend-0.0.1-SNAPSHOT.jar'
    $backend = Start-Process (Join-Path $javaHome 'bin/java.exe') -ArgumentList @('-Xmx384m', '-jar', "`"$jar`"", '--spring.jpa.show-sql=false') `
        -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $reportDir 'backend.log') `
        -RedirectStandardError (Join-Path $reportDir 'backend-error.log')
    Wait-Http 'http://localhost:8082/api/ateliers/public' $backend
    Wait-Http 'http://localhost:8082/swagger-ui.html' $backend
    $frontend = Start-Process (Get-Command node).Source -ArgumentList @('node_modules/@angular/cli/bin/ng.js',
        'serve', '--proxy-config', 'proxy.conf.json', '--host', '127.0.0.1', '--port', '4200') `
        -WorkingDirectory (Join-Path $projectRoot 'frontend') -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $reportDir 'frontend.log') -RedirectStandardError (Join-Path $reportDir 'frontend-error.log')
    Wait-Http 'http://127.0.0.1:4200/' $frontend
    Wait-Http 'http://127.0.0.1:4200/api/ateliers/public' $frontend
    $env:PLAYWRIGHT_JSON_OUTPUT_NAME = Join-Path $reportDir 'playwright.json'
    Invoke-Checked node @('node_modules/@playwright/test/cli.js', 'test', '--config', 'playwright.config.ts') 'playwright.log'
    $browserReport = Get-Content $env:PLAYWRIGHT_JSON_OUTPUT_NAME -Raw | ConvertFrom-Json
    if ($browserReport.stats.unexpected -ne 0 -or $browserReport.stats.skipped -ne 0) {
        throw 'Browser report contains failed or skipped tests.'
    }

    $fingerprintSql = "SELECT md5(COALESCE(string_agg(row_to_json(r)::text, '' ORDER BY id), '')) FROM reservations r;"
    $beforeRestart = Invoke-TestSql $browserDatabase $fingerprintSql
    Stop-OwnedProcesses
    $backend = $null
    $frontend = $null
    Invoke-Checked docker @('restart', $TestContainer) 'postgres-restart.log'
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        $ErrorActionPreference = 'Continue'
        & docker exec $TestContainer pg_isready -U $env:POSTGRES_USER *> $null
        $ready = $LASTEXITCODE -eq 0
        $ErrorActionPreference = 'Stop'
        if ($ready) { break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'Test PostgreSQL did not recover after restart.' }
    if ($beforeRestart -ne (Invoke-TestSql $browserDatabase $fingerprintSql)) {
        throw 'Persistence check failed after test PostgreSQL restart.'
    }
    $junit = @(Get-ChildItem (Join-Path $projectRoot 'target/surefire-reports/TEST-*.xml') | ForEach-Object {
        ([xml](Get-Content $_.FullName -Raw)).testsuite
    })
    $vitest = Get-Content (Join-Path $reportDir 'vitest.json') -Raw | ConvertFrom-Json
    $summary = [ordered]@{
        run = $runId; testDatabase = $testDatabase; browserDatabase = $browserDatabase
        junitTests = ($junit | Measure-Object -Property tests -Sum).Sum
        junitFailures = ($junit | Measure-Object -Property failures -Sum).Sum
        junitErrors = ($junit | Measure-Object -Property errors -Sum).Sum
        junitSkipped = ($junit | Measure-Object -Property skipped -Sum).Sum
        vitestPassed = $vitest.numPassedTests; vitestFailed = $vitest.numFailedTests
        playwrightPassed = $browserReport.stats.expected; playwrightFailed = $browserReport.stats.unexpected
        persistenceAfterRestart = $true; reportDirectory = $reportDir
    }
    $summary | ConvertTo-Json | Set-Content (Join-Path $reportDir 'summary.json') -Encoding UTF8
    Write-Host ($summary | ConvertTo-Json)
} finally {
    Stop-OwnedProcesses
    foreach ($name in $runtimeVariables) { [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name]) }
    Set-Location $originalLocation
}
