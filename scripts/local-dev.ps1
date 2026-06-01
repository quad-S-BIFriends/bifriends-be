# 로컬 Gradle/bootRun용 JAVA_HOME (프로젝트는 JDK 21 toolchain)
# 사용: . .\scripts\local-dev.ps1  후 ./gradlew bootRun

$ErrorActionPreference = "Stop"

# 잘못된 JAVA_HOME (.exe 경로 등) 제거
if ($env:JAVA_HOME -and -not (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    Write-Warning "Invalid JAVA_HOME removed: $($env:JAVA_HOME)"
    Remove-Item Env:JAVA_HOME -ErrorAction SilentlyContinue
}

$candidates = @(
    "C:\Program Files\Java\jdk-21",
    "C:\Program Files\Eclipse Adoptium\jdk-21*",
    "C:\Program Files\Microsoft\jdk-21*",
    "C:\Program Files\Java\jdk-17"
)

foreach ($pattern in $candidates) {
    $resolved = Get-Item $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($resolved -and (Test-Path (Join-Path $resolved.FullName "bin\java.exe"))) {
        $env:JAVA_HOME = $resolved.FullName
        Write-Host "JAVA_HOME=$env:JAVA_HOME"
        & (Join-Path $env:JAVA_HOME "bin\java.exe") -version
        return
    }
}

Write-Host "JDK not found in common paths. Gradle will auto-download JDK 21 via toolchain (foojay)."
Write-Host "Or install JDK 21: https://adoptium.net/temurin/releases/?version=21"
