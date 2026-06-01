package com.bifriends.domain.safety.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

/**
 * AI → BE 주간 안전 신호 콜백 요청
 *
 * AI 배치 처리 결과:
 *   chat_messages 분석 → 키워드 카운팅 → 점수 계산 → Gemini 요약 → BE 콜백
 */
data class WeeklySafetyReportRequest(
    @JsonProperty("member_id")
    val memberId: Long,

    @JsonProperty("week_start")
    val weekStart: LocalDate,

    @JsonProperty("week_end")
    val weekEnd: LocalDate,

    /** GREEN / YELLOW / RED */
    @JsonProperty("safety_signal")
    val safetySignal: String,

    @JsonProperty("score")
    val score: Int,

    /** Gemini가 생성한 한두 문장 요약 */
    @JsonProperty("reason_summary")
    val reasonSummary: String? = null,
)

data class WeeklySafetyReportResponse(
    val received: Boolean = true,
)
