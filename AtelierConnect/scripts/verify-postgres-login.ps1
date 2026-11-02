[CmdletBinding()]
param([ValidateRange(1, 5)][int]$MaxAttempts = 3, [switch]$NonInteractive)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$reportDir = Join-Path $root '.local/diagnostics'
New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
foreach ($name in @('POSTGRES_USER','POSTGRES_PASSWORD','SPRING_DATASOURCE_URL')) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) { throw "Missing variable: $name" }
}
if ($env:SPRING_DATASOURCE_URL -notmatch '^jdbc:postgresql://(localhost|127\.0\.0\.1):5441/atelierconnect_db$') {
    throw 'Expected the application database on localhost:5441. No database was modified.'
}
$ErrorActionPreference = 'Continue'
$inspection = & docker inspect atelierconnect-db 2>$null
$dockerCode = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
if ($dockerCode -ne 0) { throw 'Existing PostgreSQL container unavailable.' }
$metadata = ($inspection | ConvertFrom-Json)[0]
if (-not $metadata.State.Running -or @($metadata.NetworkSettings.Ports.'5432/tcp'.HostPort) -notcontains '5441') {
    throw 'The existing PostgreSQL container must be running with port 5441 published.'
}
# Resolve the same driver as Spring Boot, then connect from Windows.
$classpathFile = Join-Path $reportDir 'jdbc-classpath.txt'
$ErrorActionPreference = 'Continue'
& (Join-Path $root 'mvnw.cmd') -B -ntp -f (Join-Path $root 'pom.xml') dependency:build-classpath '-DincludeScope=runtime' "-Dmdep.outputFile=$classpathFile" *> (Join-Path $reportDir 'jdbc-dependencies.log')
$code = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
if ($code -ne 0) { throw 'Cannot resolve JDBC dependencies. See .local/diagnostics/jdbc-dependencies.log.' }
$separator = [regex]::Escape([string][IO.Path]::PathSeparator)
$driver = @((Get-Content $classpathFile -Raw).Trim() -split $separator | Where-Object { $_ -match '[\\/]postgresql-[\d.]+\.jar$' })
if ($driver.Count -ne 1 -or -not (Test-Path -LiteralPath $driver[0])) { throw 'Cannot identify the application PostgreSQL driver.' }
function Test-JdbcLogin {
    $ErrorActionPreference = 'Continue'
    & java --class-path $driver[0] (Join-Path $PSScriptRoot 'PostgresLoginProbe.java') *> (Join-Path $reportDir 'jdbc-login.log')
    return $LASTEXITCODE -eq 0
}
$authenticated = Test-JdbcLogin
if (-not $authenticated -and -not (Select-String -LiteralPath (Join-Path $reportDir 'jdbc-login.log') -Pattern 'SQLSTATE=28P01' -Quiet)) {
    throw 'JDBC failed for a reason other than an incorrect password. Check the SQLSTATE and JDBC_REASON in .local/diagnostics/jdbc-login.log; no password was requested or changed.'
}
if (-not $authenticated -and -not $NonInteractive) {
    Write-Warning 'JDBC from Windows failed. Enter the EXISTING password; no password will be reset.'
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        $secure = Read-Host "Existing PostgreSQL password ($attempt/$MaxAttempts)" -AsSecureString
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try { $candidate = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
        $env:POSTGRES_PASSWORD = $candidate
        $candidate = $null
        if (Test-JdbcLogin) { $authenticated = $true; break }
    }
}
if (-not $authenticated) { throw 'PostgreSQL JDBC login failed from Windows. See .local/diagnostics/jdbc-login.log (SQLSTATE only). Supply the existing password; no role or data was changed.' }
$server = & docker exec atelierconnect-db psql -X -v ON_ERROR_STOP=1 -U $env:POSTGRES_USER -d atelierconnect_db -tAc 'SELECT extract(epoch from pg_postmaster_start_time())::text'
if ($LASTEXITCODE -ne 0) { throw 'Cannot verify container server identity.' }
$identity = Get-Content (Join-Path $reportDir 'jdbc-login.log') | Where-Object { $_ -like 'SERVER_STARTED_AT=*' }
if ($identity -ne ('SERVER_STARTED_AT=' + ($server | Out-String).Trim())) { throw 'JDBC server does not match the container.' }
Write-Host '[OK] Authentification PostgreSQL depuis Windows'
Write-Host '[OK] Connexion JDBC au conteneur existant'
