package com.bifriends.domain.report.model

import com.bifriends.domain.member.model.Member
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 주간 채팅 안전 신호 (AI 배치 결과)
 *
 * 매주 월요일 01:00 KST BE 스케줄러가 AI 배치를 트리거해 분석:
 *   1. chat_messages 전체 조회
 *   2. 키워드 카운팅 (Python, Gemini 호출 없음)
 *   3. 점수 계산 → GREEN / YELLOW / RED 판정
 *   4. 위험 문장 → Gemini에 요약 요청 1회
 *   5. POST /api/v1/weekly-safety-report 로 BE에 전송
 */
@Entity
@Table(
    name = "weekly_safety_report",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_weekly_safety_member_week",
        columnNames = ["member_id", "week_start"]
    )],
    indexes = [Index(name = "idx_weekly_safety_member", columnList = "member_id, week_start DESC")]
)
class WeeklySafetyReport(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @Column(nullable = false)
    val weekStart: LocalDate,

    @Column(nullable = false)
    val weekEnd: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var safetySignal: SafetySignal,

    @Column(nullable = false)
    var score: Int,

    @Column(length = 255)
    var reasonSummary: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    fun update(safetySignal: SafetySignal, score: Int, reasonSummary: String?) {
        this.safetySignal = safetySignal
        this.score = score
        this.reasonSummary = reasonSummary
    }
}
