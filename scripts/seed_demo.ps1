# BiFriends demo seed (Windows)
# Usage: .\scripts\seed_demo.ps1 -Email "your@gmail.com"
#
# Prerequisites:
#   1. docker-compose up -d db
#   2. Log in once via the app (Google OAuth)

param(
    [Parameter(Mandatory = $true)]
    [string]$Email,

    [string]$Container = "bifriends-db",
    [string]$DbUser = "bifriends",
    [string]$DbName = "bifriends"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$SqlFile = Join-Path $ScriptDir "seed_demo.sql"
$TempSql = Join-Path $env:TEMP "bifriends_seed_demo.sql"

if (-not (Test-Path $SqlFile)) {
    Write-Error "seed_demo.sql not found: $SqlFile"
}

$running = docker ps --filter "name=$Container" --format "{{.Names}}" 2>$null
if (-not $running) {
    Write-Error "Container '$Container' is not running. Run: docker-compose up -d db"
}

$escapedEmail = $Email.Replace("'", "''")
$sql = (Get-Content $SqlFile -Raw -Encoding UTF8).Replace('__DEMO_EMAIL__', $escapedEmail)
[System.IO.File]::WriteAllText($TempSql, $sql, [System.Text.UTF8Encoding]::new($false))

Write-Host ">> Seeding demo data — email=$Email, container=$Container"

docker cp $TempSql "${Container}:/tmp/seed_demo.sql"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

docker exec $Container psql -U $DbUser -d $DbName -v ON_ERROR_STOP=1 -f /tmp/seed_demo.sql

if ($LASTEXITCODE -ne 0) {
    Remove-Item $TempSql -ErrorAction SilentlyContinue
    Write-Error "Seed failed. Check email and that you logged in via the app first."
}

Remove-Item $TempSql -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "Done! Reopen the app to verify."
Write-Host "  Parent mode PIN: 1234"
Write-Host "  available_pool: 30"
Write-Host "  22 learning attempts, 2 weekly reports, 3 chat sessions"
