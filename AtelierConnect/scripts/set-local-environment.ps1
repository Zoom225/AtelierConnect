[CmdletBinding()]
param(
    [switch]$NonInteractive,
    [switch]$ConfigureIntelliJ
)

# The optional private IntelliJ configuration is also the PowerShell source.
# Never print environment values or put them in a tracked file.
$ErrorActionPreference = 'Stop'
if ($ConfigureIntelliJ) {
    throw 'ConfigureIntelliJ is disabled: credentials must not be saved as plaintext. Supply them through the process environment or the existing private configuration.'
}
$backendRoot = Split-Path $PSScriptRoot -Parent
$privateRunFile = Join-Path $backendRoot '.run/AtelierConnect_Local.private.run.xml'
$required = @('POSTGRES_USER', 'POSTGRES_PASSWORD', 'JWT_SECRET',
    'APP_DEMO_MEMBER_PASSWORD')
$allowed = $required + @('SPRING_DATASOURCE_URL', 'APP_DEMO_ADMIN_PASSWORD')
if (Test-Path -LiteralPath $privateRunFile) {
    [xml]$privateConfiguration = Get-Content -LiteralPath $privateRunFile -Raw -Encoding UTF8
    foreach ($entry in $privateConfiguration.component.configuration.envs.env) {
        if ($entry.name -in $allowed -and -not [Environment]::GetEnvironmentVariable($entry.name)) {
            [Environment]::SetEnvironmentVariable($entry.name, $entry.value)
        }
    }
}
foreach ($name in $allowed) {
    if (-not [Environment]::GetEnvironmentVariable($name)) {
        foreach ($scope in @('User', 'Machine')) {
            $saved = [Environment]::GetEnvironmentVariable($name, $scope)
            if (-not [string]::IsNullOrWhiteSpace($saved)) {
                [Environment]::SetEnvironmentVariable($name, $saved)
                break
            }
        }
    }
}
if (-not $env:SPRING_DATASOURCE_URL) {
    $env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5441/atelierconnect_db'
}
$missing = @($required | Where-Object { -not [Environment]::GetEnvironmentVariable($_) })
if ($NonInteractive -and $missing.Count -gt 0) {
    throw ('Missing environment variables: ' + ($missing -join ', '))
}
foreach ($name in $missing) {
    if ($name -eq 'POSTGRES_USER') {
        $value = Read-Host $name
    } else {
        $secure = Read-Host "$name (existing value; never paste it in chat)" -AsSecureString
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try { $value = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
    }
    if ([string]::IsNullOrWhiteSpace($value)) { throw "Missing value for $name" }
    [Environment]::SetEnvironmentVariable($name, $value)
    $value = $null
}
if ([string]::IsNullOrWhiteSpace($env:JWT_SECRET) -or
    [Text.Encoding]::UTF8.GetByteCount($env:JWT_SECRET) -lt 32) {
    throw 'JWT_SECRET must contain at least 32 bytes, as required by the backend.'
}
Write-Host 'Local environment is ready in this terminal. No existing account password was changed.'
