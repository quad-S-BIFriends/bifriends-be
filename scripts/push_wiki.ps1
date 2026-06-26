# Push wiki/ sources to GitHub Wiki clone
# Usage:
#   .\scripts\push_wiki.ps1
#   .\scripts\push_wiki.ps1 -WikiClonePath D:\work\bifriends-be.wiki

param(
    [string]$WikiClonePath = (Join-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) "bifriends-be.wiki")
)

$ErrorActionPreference = "Stop"
$WikiSource = Join-Path $PSScriptRoot "..\wiki"
$WikiGitUrl = "https://github.com/quad-S-BIFriends/bifriends-be.wiki.git"
$WikiWebUrl = "https://github.com/quad-S-BIFriends/bifriends-be/wiki"

if (-not (Test-Path $WikiSource)) {
    Write-Error "Wiki source not found: $WikiSource"
}

function Test-WikiGitRepo {
    param([string]$Path)
    return (Test-Path (Join-Path $Path ".git"))
}

function Test-WikiInitialized {
    # REST wiki/pages API는 환경에 따라 404 — clone URL HEAD로 확인
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    git -c credential.helper= -c "credential.helper=!gh auth git-credential" ls-remote $WikiGitUrl HEAD 2>$null | Out-Null
    $ok = ($LASTEXITCODE -eq 0)
    $ErrorActionPreference = $prev
    return $ok
}

function Show-FirstPageRequired {
    Write-Host ""
    Write-Host "=== Wiki git 저장소가 아직 없습니다 ===" -ForegroundColor Yellow
    Write-Host "브라우저에서 먼저 첫 페이지를 만드세요 (터미널 URL 입력 X):"
    Write-Host "  $WikiWebUrl"
    Write-Host "  -> 'Create the first page' -> Title: Home -> Save"
    Write-Host "그 다음 다시: .\scripts\push_wiki.ps1"
    Write-Host ""
    exit 1
}

function Invoke-WikiClone {
    param([string]$Path)
    Write-Host "Cloning $WikiGitUrl ..."
    $parent = Split-Path $Path -Parent
    if (-not (Test-Path $parent)) { New-Item -ItemType Directory -Path $parent | Out-Null }
    if (Test-Path $Path) { Remove-Item -Recurse -Force $Path }

    $prev = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    git -c credential.helper= -c "credential.helper=!gh auth git-credential" clone $WikiGitUrl $Path 2>&1 | ForEach-Object { Write-Host $_ }
    $ok = ($LASTEXITCODE -eq 0)
    $ErrorActionPreference = $prev

    if (-not $ok) {
        if (-not (Test-WikiInitialized)) {
            Show-FirstPageRequired
        }
        Write-Error "Wiki git clone failed. Check gh auth (gh auth status) and repo access."
    }
}

if (-not (Test-WikiInitialized)) {
    Show-FirstPageRequired
}

if (-not (Test-WikiGitRepo $WikiClonePath)) {
    if (Test-Path $WikiClonePath) {
        Write-Host "Removing invalid wiki folder (no .git): $WikiClonePath"
        Remove-Item -Recurse -Force $WikiClonePath
    }
    Invoke-WikiClone $WikiClonePath
}

Copy-Item -Path (Join-Path $WikiSource "*") -Destination $WikiClonePath -Force -Recurse

Push-Location $WikiClonePath
try {
    git add .
    $status = git status --porcelain
    if (-not $status) {
        Write-Host "No wiki changes to commit."
        exit 0
    }
    git commit -m "docs(wiki): sync from bifriends-be/wiki"
    $branch = (git branch --show-current)
    if (-not $branch) { $branch = "master" }
    git push origin $branch
    Write-Host "Wiki pushed: $WikiWebUrl"
} finally {
    Pop-Location
}
