package com.bifriends.infrastructure.ai.dto

import com.fasterxml.jackson.annotation.JsonProperty

// ── BE → AI 요청 ──────────────────────────────────────────────────────────

data class AiEmotionScenarioRequest(
    @JsonProperty("member_id")    val memberId: Long,
    @JsonProperty("nickname")     val nickname: String,
    @JsonProperty("interests")    val interests: List<String> = emptyList(),
    @JsonProperty("learned_expressions") val learnedExpressions: List<String> = emptyList(),
    /** 감정 지정 시 해당 감정으로 생성. 미지정 시 AI가 선택. */
    @JsonProperty("emotion")      val emotion: String? = null,
    /**
     * step1·step2 폴백 이미지 URL 맵
     * AI가 폴백 시나리오 생성 시 이 맵에서 image_url을 꺼내 응답에 채운다.
     *
     * 구조: { "고마움": { "step1": "https://...", "step2": "https://..." }, ... }
     */
    @JsonProperty("fallback_urls") val fallbackUrls: Map<String, Map<String, String>> = emptyMap(),
)

// ── AI → BE 응답 ──────────────────────────────────────────────────────────

data class AiEmotionScenarioResponse(
    @JsonProperty("set_id")               val setId: String,
    @JsonProperty("emotion")              val emotion: String,
    @JsonProperty("situation")            val situation: String,
    @JsonProperty("learned_expression")   val learnedExpression: String,
    @JsonProperty("steps")                val steps: AiSteps,
    @JsonProperty("is_fallback")          val isFallback: Boolean = false,
)

data class AiSteps(
    val step1: AiStep1,
    val step2: AiStep2,
    val step3: AiStep3,
    val step4: AiStep4,
)

// ── Step 1: 오늘의 표현 배우기 ─────────────────────────────────────────────

data class AiStep1(
    val title: String,
    val expression: String,
    val emotion: String,
    @JsonProperty("body_sensation")    val bodySensation: String,
    @JsonProperty("situation_example") val situationExample: String,
    @JsonProperty("image_url")         val imageUrl: String,
    @JsonProperty("next_button_text")  val nextButtonText: String,
)

// ── Step 2: 어떤 기분일까요? ───────────────────────────────────────────────

data class AiStep2(
    val title: String,
    @JsonProperty("visual_clue")      val visualClue: String,
    val question: String,
    val choices: List<AiChoice>,
    @JsonProperty("image_url")        val imageUrl: String,
    @JsonProperty("retry_message")    val retryMessage: String,
    @JsonProperty("next_button_text") val nextButtonText: String,
)

// ── Step 3: 3컷 만화 ─────────────────────────────────────────────────────

data class AiStep3(
    val title: String,
    val comic: List<AiComicCut>,
    val question: String,
    val choices: List<AiChoice>,
    @JsonProperty("retry_message")    val retryMessage: String,
    @JsonProperty("next_button_text") val nextButtonText: String,
)

/**
 * 3컷 만화 각 컷
 *
 * - 실시간 생성: AI가 image_b64를 채워서 반환 → BE가 Firebase Storage 업로드 후 image_url로 교체
 * - 폴백 시: AI가 image_url을 직접 채워서 반환 (image_b64 없음)
 */
data class AiComicCut(
    val cut: Int,
    val text: String,
    @JsonProperty("image_prompt") val imagePrompt: String,
    /** 실시간 생성 이미지 (Base64). BE가 Storage에 업로드 후 null로 교체. */
    @JsonProperty("image_b64")    val imageB64: String? = null,
    /** Firebase Storage URL. 폴백 시 AI가 채워서 전달. */
    @JsonProperty("image_url")    val imageUrl: String? = null,
)

// ── Step 4: 이렇게 말하고 싶어! ──────────────────────────────────────────

data class AiStep4(
    val title: String,
    @JsonProperty("leo_intro")          val leoIntro: String,
    val question: String,
    val choices: List<AiStep4Choice>,
    @JsonProperty("retry_message")      val retryMessage: String,
    @JsonProperty("success_message")    val successMessage: String,
    val reward: AiReward,
    @JsonProperty("complete_button_text") val completeButtonText: String,
)

data class AiStep4Choice(
    val id: String,
    val text: String,
    val type: String,   // empathetic / indifferent / irrelevant
    @JsonProperty("is_correct") val isCorrect: Boolean,
    val feedback: String,
)

// ── 공통 ─────────────────────────────────────────────────────────────────

data class AiChoice(
    val id: String,
    val text: String,
    @JsonProperty("is_correct") val isCorrect: Boolean,
    val feedback: String,
)

data class AiReward(
    val type: String,
    val amount: Int,
)
