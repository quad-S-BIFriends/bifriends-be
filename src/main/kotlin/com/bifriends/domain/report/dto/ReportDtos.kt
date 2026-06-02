package com.bifriends.domain.report.dto

import com.bifriends.domain.report.model.SafetySignal
// import com.bifriends.domain.report.model.WeeklyReport
import java.time.LocalDate

// ── RPT-02 리포트 목록 ─────────────────────────────────────────────────────────

data class ReportSummaryItem(
    val reportId: Long,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val safetySignal: SafetySignal,
    /** 보호자 미션 수령 여부 */
    val hasMission: Boolean,
)

data class ReportListResponse(
    val reports: List<ReportSummaryItem>,
)

// ── RPT-03~07 리포트 상세 ──────────────────────────────────────────────────────

data class ReportDetailResponse(
    val reportId: Long,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,

    /** RPT-04 성장 요약 */
    val growth: GrowthResponse,

    /** RPT-05 학습 패턴 (BE 계산) */
    val learningPattern: LearningPatternResponse,

    /** RPT-06 학습 현황 */
    val learningStatus: LearningStatusResponse,

    /** RPT-07 챗 안전 신호 */
    val chatSafety: ChatSafetyResponse,

    /** RPT-08 보호자 미션 (버튼 클릭 전 null) */
    val parentMission: ParentMissionResponse?,

    /** 주요 키워드 */
    val keywords: List<String>,
)

// ── RPT-04 성장 요약 ──────────────────────────────────────────────────────────

data class GrowthResponse(
    /** AI 생성 주간 성장 요약 */
    val summary: String?,
    /** AI 생성 보호자 팁 */
    val parentTip: String?,
)

// ── RPT-05 학습 패턴 ──────────────────────────────────────────────────────────

data class LearningPatternResponse(
    /** 학습한 요일 번호 목록 (1=월 ~ 7=일) */
    val learningDays: List<Int>,
    /** 해당 주 할 일 완료 횟수 */
    val completedTodoCount: Int,
)

// ── RPT-06 학습 현황 ──────────────────────────────────────────────────────────

data class LearningStatusResponse(
    val math: SubjectStatusResponse?,
    val korean: SubjectStatusResponse?,
    val emotion: SubjectStatusResponse?,
)

data class SubjectStatusResponse(
    /** AI 생성 한 줄 요약 */
    val summary: String,
)

// ── RPT-07 챗 안전 신호 ───────────────────────────────────────────────────────

data class ChatSafetyResponse(
    val signal: SafetySignal,
    val score: Int,
    val reasonSummary: String,
)

// ── RPT-08 보호자 미션 ────────────────────────────────────────────────────────

data class ParentMissionResponse(
    /** AI 생성 칭찬 멘트 */
    val praise: String,
    /** AI 생성 추천 활동 */
    val mission: String,
)
