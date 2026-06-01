package com.bifriends.domain.report.model

import com.bifriends.domain.member.model.Member
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 주간 성장 리포트 (부모 모드 - 성장일기)
 *
 * AI → BE 콜백(POST /api/v1/weekly-safety-report)으로 수신한 데이터와
 * BE가 직접 계산한 학습 통계를 함께 저장한다.
 *
 * 저장 흐름:
 *   1. AI가 주간 분석 완료 → BE 콜백
 *   2. WeeklySafetyService가 이 엔티티에 저장
 *   3. 부모가 리포트 조회 시 여기서 읽어 반환
 *
 * 보호자 미션(RPT-08)은 부모가 버튼을 누를 때 AI에 요청 후 parentMission에 캐시 저장.
 */
@Entity
@Table(
    name = "weekly_reports",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_weekly_report_member_week",
        columnNames = ["member_id", "week_start"]
    )],
    indexes = [Index(name = "idx_weekly_reports_member", columnList = "member_id, week_start DESC")]
)
class WeeklyReport(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    /** 해당 주 월요일 */
    @Column(nullable = false)
    val weekStart: LocalDate,

    /** 해당 주 금요일 */
    @Column(nullable = false)
    val weekEnd: LocalDate,

    // ── RPT-07 챗 안전 신호 ──────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val safetySignal: SafetySignal,

    @Column(nullable = false)
    val safetyScore: Int,

    @Column(nullable = false, columnDefinition = "TEXT")
    val safetyReasonSummary: String,

    // ── RPT-04 성장 요약 (AI 생성) ──────────────────────────────────────
    /** AI가 생성한 주간 성장 요약 문장 */
    @Column(columnDefinition = "TEXT")
    val growthSummary: String? = null,

    /** AI가 생성한 보호자 팁 */
    @Column(columnDefinition = "TEXT")
    val parentTip: String? = null,

    // ── RPT-06 학습 현황 (AI 생성 한 줄 요약) ────────────────────────────
    val mathSummary: String? = null,
    val koreanSummary: String? = null,
    val emotionSummary: String? = null,

    // ── RPT-05 학습 패턴 (BE 계산) ──────────────────────────────────────
    /** 해당 주 학습한 요일 목록 (JSON 배열, 예: "[1,2,4]") */
    @Column(columnDefinition = "TEXT")
    val learningDaysJson: String? = null,

    /** 해당 주 학습 완료 횟수 */
    @Column(nullable = false)
    val learningCompletedCount: Int = 0,

    // ── RPT-08 보호자 미션 (AI 생성 - on demand 캐시) ────────────────────
    /** 보호자 미션 버튼 클릭 시 AI가 생성한 칭찬 멘트 */
    @Column(columnDefinition = "TEXT")
    var parentMissionPraise: String? = null,

    /** 보호자 미션 버튼 클릭 시 AI가 생성한 추천 활동 */
    @Column(columnDefinition = "TEXT")
    var parentMission: String? = null,

    /** 주요 감지 키워드 (JSON 배열, 예: "[\"외로움\",\"친구\"]") */
    @Column(columnDefinition = "TEXT")
    val keywordsJson: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    /** 보호자 미션 저장 (RPT-08 on-demand 캐시) */
    fun cacheParentMission(praise: String, mission: String) {
        this.parentMissionPraise = praise
        this.parentMission = mission
        this.updatedAt = LocalDateTime.now()
    }
}
