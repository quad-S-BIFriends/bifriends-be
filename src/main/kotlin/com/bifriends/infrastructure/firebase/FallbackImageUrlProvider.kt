package com.bifriends.infrastructure.firebase

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLEncoder

/**
 * 감정별 폴백 이미지 URL 제공자
 *
 * Firebase Storage에 사전 업로드된 step1·step2 고정 이미지의 URL을
 * 토큰 + 감정명으로 동적으로 조합하여 반환한다.
 *
 * ── URL 조합 방식 ─────────────────────────────────────────────────────────
 * 패턴: https://firebasestorage.googleapis.com/v0/b/{bucket}/o/{encoded_path}?alt=media&token={token}
 * 경로: fallback%2F{URL인코딩된 감정명}%2F{step}.png
 *
 * 예) 감정="고마움", step="step1"
 * → https://firebasestorage.googleapis.com/v0/b/bifriends-5df72.firebasestorage.app
 *   /o/fallback%2F%EA%B3%A0%EB%A7%88%EC%9B%80%2Fstep1.png?alt=media&token={token}
 * ────────────────────────────────────────────────────────────────────────
 *
 * ── 설정 키 → 감정명 매핑 ───────────────────────────────────────────────
 * gomaeum      → 고마움
 * gippeum      → 기쁨
 * bukk         → 부끄러움
 * soksanghom   → 속상함
 * silmang      → 실망
 * hwanam       → 화남
 * ────────────────────────────────────────────────────────────────────────
 */
@Component
class FallbackImageUrlProvider(
    private val props: FallbackImageProperties,
    @Value("\${firebase.storage.bucket}") private val bucket: String,
) {

    companion object {
        private const val FIREBASE_BASE = "https://firebasestorage.googleapis.com/v0/b"

        /**
         * 한국어 감정명 → application.yml 설정 키(romanized) 매핑
         * AI가 반환하는 감정명(한국어)을 기반으로 토큰 키를 찾는다.
         */
        private val EMOTION_TO_KEY = mapOf(
            "고마움"   to "gomaeum",
            "기쁨"    to "gippeum",
            "부끄러움" to "bukk",
            "속상함"   to "soksanghom",
            "실망"    to "silmang",
            "화남"    to "hwanam",
        )

        /** 지원하는 감정 목록 (AI 요청 시 폴백 URL 맵 생성에 사용) */
        val SUPPORTED_EMOTIONS: List<String> = EMOTION_TO_KEY.keys.toList()
    }

    // ── 공개 API ──────────────────────────────────────────────────────────

    /** step1 (상반신 이미지) URL 반환 */
    fun getStep1Url(emotion: String): String = buildUrl(emotion, "step1")

    /** step2 (얼굴 클로즈업 이미지) URL 반환 */
    fun getStep2Url(emotion: String): String = buildUrl(emotion, "step2")

    /**
     * step3 폴백 만화 컷 이미지 URL 반환
     *
     * Firebase Storage 파일명: step3-{cut}.png (예: step3-1.png, step3-2.png, step3-3.png)
     *
     * @param cut 컷 번호 (1, 2, 3)
     */
    fun getStep3Url(emotion: String, cut: Int): String = buildUrl(emotion, "step3-$cut")

    /**
     * AI 요청 body에 포함할 감정별 폴백 URL 전체 맵을 반환한다.
     *
     * AI는 폴백 시나리오 생성 시 이 맵에서 각 step의 URL을 꺼내
     * 응답의 `image_url` 필드에 채운다.
     *
     * 반환 구조:
     * ```json
     * {
     *   "고마움": {
     *     "step1":   "https://...step1.png?...",
     *     "step2":   "https://...step2.png?...",
     *     "step3-1": "https://...step3-1.png?...",
     *     "step3-2": "https://...step3-2.png?...",
     *     "step3-3": "https://...step3-3.png?..."
     *   },
     *   ...
     * }
     * ```
     */
    fun buildFallbackUrlMap(): Map<String, Map<String, String>> {
        return SUPPORTED_EMOTIONS.associateWith { emotion ->
            mapOf(
                "step1"   to getStep1Url(emotion),
                "step2"   to getStep2Url(emotion),
                "step3-1" to getStep3Url(emotion, 1),
                "step3-2" to getStep3Url(emotion, 2),
                "step3-3" to getStep3Url(emotion, 3),
            )
        }
    }

    // ── 내부 ─────────────────────────────────────────────────────────────

    private fun buildUrl(emotion: String, step: String): String {
        val key = EMOTION_TO_KEY[emotion]
            ?: throw IllegalArgumentException("지원하지 않는 감정입니다: $emotion")

        val token = props.tokens["$key-$step"]?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "폴백 이미지 토큰이 설정되지 않았습니다: $key-$step\n" +
                "환경변수 FALLBACK_TOKEN_${key.uppercase()}_${step.uppercase().replace("-", "")}를 확인하세요."
            )

        // Firebase Storage URL 경로 구성
        // URLEncoder: 한국어 → %EA%B3%A0... 형식, '+' → '%20' 교체 필요
        val encodedEmotion = URLEncoder.encode(emotion, "UTF-8").replace("+", "%20")
        val encodedPath = "fallback%2F$encodedEmotion%2F$step.png"

        return "$FIREBASE_BASE/$bucket/o/$encodedPath?alt=media&token=$token"
    }
}
