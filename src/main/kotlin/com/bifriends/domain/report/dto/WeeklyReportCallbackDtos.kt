package com.bifriends.domain.report.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

/**
 * AI → BE 주간 성장 리포트 콜백 (4섹션 JSON)
 *
 * AI가 생성한 sections JSON을 통째로 받아 weekly_report 테이블에 저장한다.
 */
data class WeeklyReportCallbackRequest(
    @JsonProperty("member_id")
    val memberId: Long,

    @JsonProperty("week_start")
    val weekStart: LocalDate,

    @JsonProperty("week_end")
    val weekEnd: LocalDate,

    /**
     * AI가 생성한 4섹션 JSON 문자열
     * {
     *   "growth":         { "summary": "...", "parentTip": "..." },
     *   "learningStatus": { "math": "...", "korean": "...", "emotion": "..." },
     *   "chatSafety":     { "signal": "GREEN", "score": 2, "reasonSummary": "..." },
     *   "parentMission":  null  // 버튼 클릭 시 별도 업데이트
     * }
     */
    val sections: String,
)

data class WeeklyReportCallbackResponse(
    val received: Boolean = true,
)
