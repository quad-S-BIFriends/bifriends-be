package com.bifriends.infrastructure.ai.dto

import com.bifriends.domain.report.model.SafetySignal
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * BE → AI 보호자 미션 생성 요청 (RPT-08)
 * 리포트 컨텍스트를 AI에 전달하면, AI가 칭찬 멘트와 추천 활동을 생성한다.
 */
data class AiParentMissionRequest(
    @JsonProperty("member_id")
    val memberId: Long,

    @JsonProperty("week_start")
    val weekStart: String,

    @JsonProperty("safety_signal")
    val safetySignal: String,

    @JsonProperty("growth_summary")
    val growthSummary: String?,

    @JsonProperty("math_summary")
    val mathSummary: String?,

    @JsonProperty("korean_summary")
    val koreanSummary: String?,

    @JsonProperty("emotion_summary")
    val emotionSummary: String?,

    val keywords: List<String> = emptyList(),
)

/**
 * AI → BE 보호자 미션 응답
 */
data class AiParentMissionResponse(
    /** AI 생성 칭찬 멘트 */
    val praise: String,
    /** AI 생성 추천 활동 */
    val mission: String,
)
