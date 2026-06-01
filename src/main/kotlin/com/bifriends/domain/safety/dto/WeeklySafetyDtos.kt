package com.bifriends.domain.safety.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

/**
 * AI → BE 주간 안전 보고서 콜백 요청 (확장 버전)
 *
 * AI가 주간 채팅 분석을 완료한 뒤 BE로 전송한다.
 * BE는 이 데이터를 WeeklyReport 테이블에 저장하고, 부모 모드 리포트 조회에 사용한다.
 */
data class WeeklySafetyReportRequest(

    // ── 기본 식별 정보 ──────────────────────────────────────────────────
    @JsonProperty("member_id")
    val memberId: Long,

    /** 해당 주 금요일 (report_date = week_end) */
    @JsonProperty("report_date")
    val reportDate: LocalDate,

    // ── RPT-07 챗 안전 신호 ─────────────────────────────────────────────
    /** GREEN / YELLOW / RED */
    @JsonProperty("safety_signal")
    val safetySignal: String,

    /** 점수 (0~이상, 점수 기준으로 signal 결정) */
    @JsonProperty("safety_score")
    val safetyScore: Int = 0,

    /** 안전 신호 요약 문장 */
    @JsonProperty("safety_reason_summary")
    val safetyReasonSummary: String = "",

    // ── RPT-04 성장 요약 (AI 생성) ──────────────────────────────────────
    /** 주간 성장 요약 문장 */
    @JsonProperty("growth_summary")
    val growthSummary: String? = null,

    /** 보호자에게 전달하는 팁 */
    @JsonProperty("parent_tip")
    val parentTip: String? = null,

    // ── RPT-06 학습 현황 한 줄 요약 (AI 생성) ────────────────────────────
    @JsonProperty("math_summary")
    val mathSummary: String? = null,

    @JsonProperty("korean_summary")
    val koreanSummary: String? = null,

    @JsonProperty("emotion_summary")
    val emotionSummary: String? = null,

    // ── 기존 호환 필드 ──────────────────────────────────────────────────
    /** 주요 감지 키워드 */
    val keywords: List<String> = emptyList(),

    /**
     * 기존 safety_level 필드 (하위 호환용, 새 필드 정착 후 제거 예정)
     * safety_signal 미전송 시 fallback으로 사용
     */
    @JsonProperty("safety_level")
    val safetyLevel: SafetyLevel? = null,
)

enum class SafetyLevel {
    SAFE,     // 이상 없음 → GREEN
    CONCERN,  // 주의 필요 → YELLOW
    DANGER,   // 즉각 조치 필요 → RED
}

/** BE 응답 */
data class WeeklySafetyReportResponse(
    val received: Boolean = true,
)
