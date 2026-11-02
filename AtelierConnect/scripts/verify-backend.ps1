[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$StartupLog,
    [ValidateRange(1, 180)][int]$TimeoutSeconds = 90
)
$ErrorActionPreference = 'Stop'
function Get-WindowsHttp {
    param([string]$Url)
    # curl.exe follows redirects with an explicit overall timeout and bypasses
    # machine proxy settings for this loopback-only verification.
    $body = [IO.Path]::GetTempFileName()
    try {
        $status = & curl.exe --silent --show-error --noproxy '*' --max-time 10 --location --output $body --write-out '%{http_code}' $Url
        if ($LASTEXITCODE -ne 0) { throw 'Windows HTTP request failed.' }
        return [pscustomobject]@{ StatusCode = [int]$status; Content = [IO.File]::ReadAllText($body) }
    } finally { Remove-Item -LiteralPath $body -ErrorAction SilentlyContinue }
}
$base = 'http://127.0.0.1:8082'
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$ready = $false
do {
    if (Test-Path -LiteralPath $StartupLog) {
        if (Select-String -LiteralPath $StartupLog -Pattern 'APPLICATION FAILED TO START|JAVA_EXIT_CODE=' -Quiet) {
            throw 'Spring Boot exited or failed during startup. See the sanitized startup log.'
        }
    }
    try {
        $response = Get-WindowsHttp "$base/v3/api-docs"
        $ready = $response.StatusCode -eq 200
    } catch { }
    if (-not $ready) { Start-Sleep -Seconds 1 }
} while (-not $ready -and (Get-Date) -lt $deadline)
if (-not $ready) { throw 'OpenAPI did not return HTTP 200 on Windows port 8082.' }
if (-not (Select-String -LiteralPath $StartupLog -Pattern 'Started AtelierConnectApplication' -Quiet)) {
    throw 'Missing Started AtelierConnectApplication in the supplied startup log.'
}
$listener = @(Get-NetTCPConnection -LocalPort 8082 -State Listen)
$backendId = $listener[0].OwningProcess
if (-not (Select-String -LiteralPath $StartupLog -Pattern "\b$backendId\s+---.*Started AtelierConnectApplication" -Quiet)) {
    throw 'Startup log does not confirm the PID currently listening on port 8082.'
}
$document = $response.Content | ConvertFrom-Json
if ($document.openapi -notmatch '^3\.\d+\.\d+$' -or -not $document.info.title -or
    -not $document.info.version -or -not $document.paths -or
    @($document.paths.PSObject.Properties).Count -eq 0) { throw 'Invalid or empty OpenAPI document.' }
Write-Host "[OK] Started AtelierConnectApplication; listening PID=$backendId"
Write-Host "[OK] /v3/api-docs HTTP 200; OpenAPI $($document.openapi); paths=$(@($document.paths.PSObject.Properties).Count)"
# Inspect the initial redirect separately, then follow it using Windows HTTP.
$request = [Net.HttpWebRequest]::Create("$base/swagger-ui.html")
$request.Proxy = $null
$request.Timeout = 10000
$request.AllowAutoRedirect = $false
$initial = $request.GetResponse()
try {
    $status = [int]$initial.StatusCode
    $location = $initial.Headers['Location']
    if ($status -notin @(301,302,303,307,308)) { throw "Expected Swagger redirect, got $status" }
    Write-Host "[OK] /swagger-ui.html HTTP $status -> $location"
} finally { $initial.Close() }
$ui = Get-WindowsHttp "$base/swagger-ui.html"
if ($ui.StatusCode -ne 200 -or $ui.Content -notmatch 'swagger-ui-bundle.js') { throw 'Swagger UI HTML unavailable.' }
$index = Get-WindowsHttp "$base/swagger-ui/index.html"
if ($index.StatusCode -ne 200) { throw 'Swagger index unavailable.' }
$public = Get-WindowsHttp "$base/api/ateliers/public"
if ($public.StatusCode -ne 200) { throw "Public API returned HTTP $($public.StatusCode)." }
$null = ConvertFrom-Json -InputObject $public.Content
Write-Host '[OK] /swagger-ui/index.html and /api/ateliers/public HTTP 200'
foreach ($path in @('/swagger-ui/swagger-ui-bundle.js', '/swagger-ui/swagger-ui.css',
    '/swagger-ui/swagger-initializer.js', '/v3/api-docs/swagger-config')) {
    $asset = Get-WindowsHttp "$base$path"
    if ($asset.StatusCode -ne 200) { throw "Swagger resource failed: $path" }
    if ($path -like '*swagger-config') {
        $config = $asset.Content | ConvertFrom-Json
        if ($config.url -ne '/v3/api-docs') { throw 'Unexpected Swagger OpenAPI URL.' }
    }
    Write-Host "[OK] $path HTTP 200"
}
Start-Sleep -Seconds 3
$final = Get-WindowsHttp "$base/v3/api-docs"
if ($final.StatusCode -ne 200 -or
    -not (Get-NetTCPConnection -LocalPort 8082 -State Listen | Where-Object OwningProcess -eq $backendId)) {
    throw 'Backend stopped or was replaced during verification.'
}
Get-Process -Id $backendId -ErrorAction Stop | Out-Null
Write-Host "[OK] Swagger UI after redirect HTTP 200; backend still active PID=$backendId"
