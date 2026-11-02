[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
& (Join-Path $PSScriptRoot 'set-local-environment.ps1') -NonInteractive
& (Join-Path $PSScriptRoot 'verify-postgres-login.ps1') -NonInteractive

# Use the value loaded from the private environment; never embed local passwords.
$classpath = (Get-Content (Join-Path $root '.local/diagnostics/jdbc-classpath.txt') -Raw).Trim()
& java --class-path $classpath (Join-Path $PSScriptRoot 'SetLocalParticipantPasswords.java')
if ($LASTEXITCODE -ne 0) { throw 'Local participant password reset failed.' }
$privateRunFile = Join-Path $root '.run/AtelierConnect_Local.private.run.xml'
if (Test-Path -LiteralPath $privateRunFile) {
    # Change only the participant demonstration value already stored locally.
    # Preserve all other configuration text and existing file permissions.
    $original = [IO.File]::ReadAllText($privateRunFile)
    $pattern = '(<env\b(?=[^>]*\bname="APP_DEMO_MEMBER_PASSWORD")[^>]*\bvalue=")[^"]*(")'
    if ([regex]::Matches($original, $pattern).Count -ne 1) {
        throw 'Participant passwords changed, but the private launch configuration needs manual review.'
    }
    $escaped = [Security.SecurityElement]::Escape($env:APP_DEMO_MEMBER_PASSWORD)
    $updated = [regex]::Replace($original, $pattern, [Text.RegularExpressions.MatchEvaluator]{
        param($match)
        return $match.Groups[1].Value + $escaped + $match.Groups[2].Value
    })
    if ($updated -cne $original) {
        [IO.File]::WriteAllText($privateRunFile, $updated, (New-Object Text.UTF8Encoding($false)))
    }
    Write-Host 'Local IntelliJ participant demonstration password updated. Restart the backend to load it.'
}
Write-Host 'Local participant passwords updated and verified (BCrypt).'
