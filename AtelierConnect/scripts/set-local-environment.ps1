[CmdletBinding()]
param([switch]$NonInteractive)

# Variables stay in the current PowerShell process. No secret is printed or saved.
$ErrorActionPreference = 'Stop'
if (-not $env:SPRING_DATASOURCE_URL) {
    $env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5441/atelierconnect_db'
}
$required = @('POSTGRES_USER', 'POSTGRES_PASSWORD', 'JWT_SECRET',
    'APP_DEMO_MEMBER_PASSWORD', 'APP_DEMO_ADMIN_PASSWORD')
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
if ([Text.Encoding]::UTF8.GetByteCount($env:JWT_SECRET) -lt 32) {
    throw 'JWT_SECRET must contain at least 32 bytes, as required by the backend.'
}
Write-Host 'Local environment is ready in this terminal. Existing variables were preserved; no passwords were changed.'
