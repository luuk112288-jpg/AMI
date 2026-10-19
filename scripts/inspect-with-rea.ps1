param(
    [Parameter(Mandatory = $true)][string]$ApkPath,
    [ValidateSet('inspect-android-package', 'analyze-javascript-application')][string]$Command = 'inspect-android-package'
)
$ErrorActionPreference = 'Stop'
if (-not (Get-Command npx -ErrorAction SilentlyContinue)) { throw 'Install Node.js 24.11+ with npm/npx before running REA.' }
$target = (Resolve-Path -LiteralPath $ApkPath).Path
$projectRoot = Split-Path $PSScriptRoot -Parent
$captureDir = Join-Path $projectRoot 'captures'
New-Item -ItemType Directory -Force $captureDir | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$outputFile = Join-Path $captureDir "$stamp-rea.json"
& npx --yes rea-agents@6.7.0 $Command $target --json | Out-File -LiteralPath $outputFile -Encoding utf8
if ($LASTEXITCODE -ne 0) { throw "REA failed. Inspect $outputFile; this is not successful analysis evidence." }
Write-Output "REA output saved to $outputFile. Review reported prerequisites and coverage before drawing conclusions."
