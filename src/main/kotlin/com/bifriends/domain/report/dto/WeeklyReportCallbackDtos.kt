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
     *   "growth_summary": "...",
     *   "math":           { "well_done": "...", "struggled": "..." },
     *   "korean":         { "well_done": "...", "struggled": "..." },
     *   "parent_mission": { "praise": "...", "activity": "..." }
     * }
     */
    val sections: String,
)

data class WeeklyReportCallbackResponse(
    val received: Boolean = true,
)
