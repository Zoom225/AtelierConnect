[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
& (Join-Path $PSScriptRoot 'set-local-environment.ps1') -NonInteractive
& (Join-Path $PSScriptRoot 'verify-postgres-login.ps1') -NonInteractive
$classpath = (Get-Content (Join-Path $root '.local/diagnostics/jdbc-classpath.txt') -Raw).Trim()
& java --class-path $classpath (Join-Path $PSScriptRoot 'SetLocalAdminPasswords.java')
if ($LASTEXITCODE -ne 0) { throw 'Local administrator password reset failed.' }
Write-Host 'Local administrators now log in with their email and their matricule as password (BCrypt).'
