[CmdletBinding()]
param([ValidateRange(1, 5)][int]$MaxAttempts = 3)

# This verifies the actual PostgreSQL role password over TCP (not a trust
# authenticated local Unix socket). No database, role or volume is changed.
$ErrorActionPreference = 'Stop'
$container = 'atelierconnect-db'
$db = 'atelierconnect_db'

function Test-Login {
    param([string]$Password)
    $ErrorActionPreference = 'Continue'
    # Forward password via stdin, not the command line or a tracked file.
    # The PostgreSQL client runs inside the existing application container.
    $result = $Password | & docker exec -i $container sh -c 'IFS= read -r PGPASSWORD; export PGPASSWORD; exec psql -h 127.0.0.1 -U "$POSTGRES_USER" -d atelierconnect_db -X -v ON_ERROR_STOP=1 -tAc "SELECT 1"' 2>$null
    $code = $LASTEXITCODE
    return ($code -eq 0 -and (($result | Out-String).Trim() -eq '1'))
}

if ($env:POSTGRES_USER -ne 'atelierconnect') {
    throw "Expected POSTGRES_USER=atelierconnect for the current local Docker setup. Verify your existing database role before proceeding."
}

$running = & docker inspect $container --format '{{.State.Running}}' 2>$null
if ($LASTEXITCODE -ne 0 -or (($running | Out-String).Trim() -ne 'true')) {
    throw "PostgreSQL container $container is not running."
}

# Check the published host port as well as authentication within the container.
$tcp = New-Object Net.Sockets.TcpClient
try {
    $pending = $tcp.BeginConnect('127.0.0.1', 5441, $null, $null)
    if (-not $pending.AsyncWaitHandle.WaitOne(3000, $false)) {
        throw 'Host PostgreSQL port 5441 is not reachable.'
    }
    $tcp.EndConnect($pending)
} finally {
    $tcp.Dispose()
}

if (Test-Login $env:POSTGRES_PASSWORD) {
    Write-Host 'PostgreSQL TCP authentication OK; application database preserved.' -ForegroundColor Green
    return
}

Write-Warning 'The current POSTGRES_PASSWORD is rejected by PostgreSQL (SQLState 28P01).'
for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
    $secure = Read-Host "Existing PostgreSQL role password (attempt $attempt/$MaxAttempts; hidden)" -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        $candidate = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
    if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Login $candidate)) {
        $env:POSTGRES_PASSWORD = $candidate
        $candidate = $null
        Write-Host 'PostgreSQL TCP authentication OK. Correct password applied to this launcher process.' -ForegroundColor Green
        return
    }
    $candidate = $null
    Write-Warning 'Authentication rejected; no database credentials were changed.'
}

throw @'
PostgreSQL refused all supplied passwords; Spring Boot cannot start.
Find the original password for the existing PostgreSQL volume.
If the application role has sufficient privileges, the administrator can use
  docker exec -it atelierconnect-db psql -U atelierconnect -d atelierconnect_db
then inside psql: \password atelierconnect
Only do so with the database owner's approval; it changes the role password.
Never run docker compose down -v or delete/reinitialize the database.
'@
