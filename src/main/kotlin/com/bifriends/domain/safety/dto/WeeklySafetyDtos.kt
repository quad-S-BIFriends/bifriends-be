package com.bifriends.domain.safety.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

/**
 * AI → BE 주간 안전 보고서 콜백 요청
 *
 * AI가 주간 채팅 분석을 완료한 뒤 BE로 전송한다.
 * 필드 구조는 AI 팀과 명세 확정 후 업데이트 필요.
 */
data class WeeklySafetyReportRequest(
    /** 보고서 대상 회원 ID */
    @JsonProperty("member_id")
    val memberId: Long,

    /** 분석 기준 날짜 (해당 주 금요일) */
    @JsonProperty("report_date")
    val reportDate: LocalDate,

    /** 안전 수준: SAFE / CONCERN / DANGER */
    @JsonProperty("safety_level")
    val safetyLevel: SafetyLevel,

    /** 주간 대화 요약 */
    val summary: String,

    /** 주요 감지 키워드 (선택) */
    val keywords: List<String> = emptyList(),
)

enum class SafetyLevel {
    SAFE,     // 이상 없음
    CONCERN,  // 주의 필요
    DANGER,   // 즉각 조치 필요
}

/** BE 응답 */
data class WeeklySafetyReportResponse(
    val received: Boolean = true,
)
