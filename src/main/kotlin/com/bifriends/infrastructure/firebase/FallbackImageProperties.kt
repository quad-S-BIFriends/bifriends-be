package com.bifriends.infrastructure.firebase

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 친구랑 탭 폴백 이미지 다운로드 토큰 설정
 *
 * Firebase Storage에 사전 업로드된 step1·step2 감정별 고정 이미지의 토큰을 관리한다.
 * URL 자체는 저장하지 않고 **토큰만** 환경변수로 주입받아, 코드에서 URL을 조합한다.
 *
 * ── 경로 규칙 ─────────────────────────────────────────────────────────────
 * Firebase Storage: fallback/{감정명}/{step}.png
 * 예)  fallback/고마움/step1.png
 *      fallback/화남/step2.png
 *
 * ── 설정 키 규칙 ─────────────────────────────────────────────────────────
 * yaml 키: {romanized}-{step}  (예: gomaeum-step1)
 * 환경변수: FALLBACK_TOKEN_{ROMANIZED}_STEP{N}  (예: FALLBACK_TOKEN_GOMAEUM_STEP1)
 * ────────────────────────────────────────────────────────────────────────
 *
 * ── application.yml 예시 ─────────────────────────────────────────────────
 * firebase:
 *   fallback:
 *     tokens:
 *       gomaeum-step1: ${FALLBACK_TOKEN_GOMAEUM_STEP1:}
 *       gomaeum-step2: ${FALLBACK_TOKEN_GOMAEUM_STEP2:}
 *       ...
 * ────────────────────────────────────────────────────────────────────────
 */
@ConfigurationProperties(prefix = "firebase.fallback")
data class FallbackImageProperties(
    /**
     * 감정 로마자 키 + step → Firebase Storage 다운로드 토큰 (UUID)
     * 키 형식: "{romanized}-{step}" (예: "gomaeum-step1")
     */
    val tokens: Map<String, String> = emptyMap(),
)
