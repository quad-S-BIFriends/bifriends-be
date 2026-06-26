#!/usr/bin/env bash
# STAR용 Hikari 커넥션 대기 비교 테스트 (Docker + JDK 21)
# Usage: ./scripts/chat_hikari_pool_test.sh
#
# 첫 실행: 이미지 pull + 의존성 + 컴파일 (5~15분 가능)
# 재실행: Gradle 캐시로 2~5분대

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "=== Chat Hikari Pool STAR test (Docker) ==="
echo "Project: $ROOT"
echo "(첫 실행은 오래 걸릴 수 있습니다)"

docker run --rm \
  -v "${ROOT}:/app" \
  -v bifriends-gradle-cache:/home/gradle/.gradle \
  -w /app \
  gradle:8.13-jdk21 \
  gradle test \
    --tests "com.bifriends.domain.chat.ChatHikariPoolComparisonTest" \
    --no-daemon
