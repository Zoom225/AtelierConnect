[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$StartupLog)

# Child process inherits credentials in memory. Never put them in arguments/files.
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root
$secrets = @(Get-ChildItem Env: | Where-Object Name -match '(?i)PASSWORD|SECRET|TOKEN|KEY' | ForEach-Object Value | Where-Object { $_ })
$writer = New-Object IO.StreamWriter($StartupLog, $false, (New-Object Text.UTF8Encoding($false)))
$writer.AutoFlush = $true
try {
    $ErrorActionPreference = 'Continue'
    & java '-Dspring.devtools.restart.enabled=false' -jar (Join-Path $root 'target/atelierconnect-backend-0.0.1-SNAPSHOT.jar') '--spring.jpa.show-sql=false' '--logging.level.org.hibernate.SQL=OFF' '--logging.level.org.hibernate.orm.jdbc.bind=OFF' 2>&1 | ForEach-Object {
        $line = [string]$_
        foreach ($secret in $secrets) { $line = $line.Replace($secret, '[REDACTED]') }
        $line = $line -replace '(?i)((?:password|secret|token)\s*[=:]\s*)\S+', '$1[REDACTED]'
        $writer.WriteLine($line)
    }
    $code = $LASTEXITCODE
    $writer.WriteLine("JAVA_EXIT_CODE=$code")
} finally { $writer.Dispose() }
exit $code
