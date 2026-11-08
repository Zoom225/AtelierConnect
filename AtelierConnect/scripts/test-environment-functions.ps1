# Shared by the isolated validation scripts. Values stay in process memory.
function New-TestSecret {
    $bytes = New-Object byte[] 48
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes)
}

function Wait-TestPostgres {
    param(
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][string]$User,
        [int]$TimeoutSeconds = 60
    )
    $ErrorActionPreference = 'Continue'
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        & docker exec $Container pg_isready -q -U $User -d postgres *> $null
        if ($LASTEXITCODE -eq 0) { return }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)
    throw "Isolated PostgreSQL did not become ready: $Container"
}
