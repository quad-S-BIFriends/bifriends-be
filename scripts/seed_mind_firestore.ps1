# BiFriends mindSessions Firestore demo seed (Windows)
# Usage: .\scripts\seed_mind_firestore.ps1 -Email "your@gmail.com"

param(
    [Parameter(Mandatory = $true)]
    [string]$Email,

    [int]$MemberId = 0,
    [string]$DatabaseId = $env:FIRESTORE_DATABASE_ID,
    [string]$DockerContainer = "bifriends-db"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$BeRoot = Split-Path -Parent $ScriptDir
$PyScript = Join-Path $ScriptDir "seed_mind_firestore.py"
$ReqFile = Join-Path $ScriptDir "requirements-seed.txt"

if (-not $DatabaseId) { $DatabaseId = "bifriends" }

if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
    Write-Error "Python 3 is required. Install from https://python.org"
}

Write-Host ">> Checking firebase-admin..."
python -c "import firebase_admin" 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host ">> Installing firebase-admin..."
    python -m pip install -r $ReqFile
}

$args = @($PyScript, "--email", $Email, "--database-id", $DatabaseId, "--docker-container", $DockerContainer)
if ($MemberId -gt 0) {
    $args += @("--member-id", $MemberId)
}

Push-Location $BeRoot
try {
    python @args
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
    Pop-Location
}
