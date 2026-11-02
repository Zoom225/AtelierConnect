[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$container = 'atelierconnect-validation-db'
$metadata = (docker inspect $container | ConvertFrom-Json)[0]
if ($LASTEXITCODE -ne 0 -or -not $metadata.State.Running -or
    $metadata.Config.Image -ne 'postgres:15' -or
    $metadata.Config.Labels.'atelierconnect.validation' -ne 'true' -or
    @($metadata.NetworkSettings.Ports.'5432/tcp'.HostPort) -notcontains '5549') {
    throw 'A running, labelled validation PostgreSQL 15 container on port 5549 is required.'
}
$names = @('POSTGRES_USER','POSTGRES_PASSWORD','SPRING_DATASOURCE_URL','JWT_SECRET',
    'APP_DEMO_MEMBER_PASSWORD','APP_DEMO_ADMIN_PASSWORD','RUN_POSTGRES_CONCURRENCY_TEST',
    'APP_DATA_INITIALIZE','SPRING_JPA_HIBERNATE_DDL_AUTO')
$saved = @{}
foreach ($name in $names) { $saved[$name] = [Environment]::GetEnvironmentVariable($name) }
$database = 'atelierconnect_test_' + (Get-Date -Format 'yyyyMMdd_HHmmss_fff')
$report = Join-Path $root '.local/diagnostics'
New-Item -ItemType Directory -Force $report | Out-Null
try {
    foreach ($name in @('POSTGRES_USER','POSTGRES_PASSWORD')) {
        $entry = @($metadata.Config.Env | Where-Object { $_.StartsWith($name + '=') })
        if ($entry.Count -ne 1) { throw "Missing test container variable: $name" }
        [Environment]::SetEnvironmentVariable($name, $entry[0].Substring($name.Length + 1))
    }
    & docker exec $container createdb -U $env:POSTGRES_USER $database
    if ($LASTEXITCODE -ne 0) { throw 'Cannot create isolated test database.' }
    $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://127.0.0.1:5549/$database"
    foreach ($name in @('JWT_SECRET','APP_DEMO_MEMBER_PASSWORD','APP_DEMO_ADMIN_PASSWORD')) {
        $bytes = New-Object byte[] 48
        $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
        [Environment]::SetEnvironmentVariable($name, [Convert]::ToBase64String($bytes))
    }
    $env:RUN_POSTGRES_CONCURRENCY_TEST = 'true'
    $env:APP_DATA_INITIALIZE = 'true'
    $env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'update'
    Write-Host "Testing on dedicated container $container; database $database"
    $ErrorActionPreference = 'Continue'
    & (Join-Path $root 'mvnw.cmd') -B -ntp -f (Join-Path $root 'pom.xml') clean verify *> (Join-Path $report 'backend-tests.log')
    $code = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($code -ne 0) { throw 'Backend tests failed. See .local/diagnostics/backend-tests.log.' }
    $suites = @(Get-ChildItem (Join-Path $root 'target/surefire-reports/TEST-*.xml') | ForEach-Object {
        ([xml](Get-Content $_.FullName -Raw)).testsuite
    })
    $summary = [ordered]@{ database=$database; container=$container }
    foreach ($field in @('tests','failures','errors','skipped')) {
        $summary[$field] = ($suites | Measure-Object -Property $field -Sum).Sum
    }
    $summary | ConvertTo-Json | Set-Content (Join-Path $report 'backend-tests-summary.json')
    Write-Host ($summary | ConvertTo-Json)
} finally {
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $saved[$name]) }
}
