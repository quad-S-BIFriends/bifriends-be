# STAR용 Hikari 커넥션 대기 비교 테스트 (Docker + JDK 21)
# Usage: .\scripts\chat_hikari_pool_test.ps1
#
# 첫 실행은 5~15분 걸릴 수 있음 (이미지 pull + 의존성 다운로드 + 전체 컴파일).
# 두 번째부터는 Gradle 캐시 덕분에 보통 2~5분.

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

Write-Host "=== Chat Hikari Pool STAR test (Docker) ===" -ForegroundColor Cyan
Write-Host "Project: $Root"
Write-Host "(첫 실행은 gradle 이미지/의존성 다운로드로 오래 걸릴 수 있습니다)" -ForegroundColor DarkYellow

docker run --rm `
  -v "${Root}:/app" `
  -v bifriends-gradle-cache:/home/gradle/.gradle `
  -w /app `
  gradle:8.13-jdk21 `
  gradle test `
    --tests "com.bifriends.domain.chat.ChatHikariPoolComparisonTest" `
    --no-daemon 2>&1 | Tee-Object -Variable output

if ($LASTEXITCODE -ne 0) {
    Write-Error "Test failed (exit $LASTEXITCODE)"
}

$inStar = $false
$output | ForEach-Object {
    if ($_ -match '\[STAR\]') { $inStar = $true }
    if ($inStar) { Write-Host $_ -ForegroundColor Green }
    if ($inStar -and $_ -match '^={10,}') { $inStar = $false }
}
