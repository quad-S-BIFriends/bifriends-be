package com.bifriends.domain.report.model

import com.bifriends.domain.member.model.Member
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 부모가 보는 주간 성장 리포트 (AI 생성 4섹션 JSON 통째 저장)
 *
 * sections JSON 구조 (AI가 생성):
 * {
 *   "growth":          { "summary": "...", "parentTip": "..." },
 *   "learningStatus":  { "math": "...", "korean": "...", "emotion": "..." },
 *   "chatSafety":      { "signal": "GREEN", "score": 2, "reasonSummary": "..." },
 *   "parentMission":   { "praise": "...", "mission": "..." }  // 버튼 클릭 시 추가
 * }
 */
@Entity
@Table(
    name = "weekly_report",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_weekly_report_member_week",
        columnNames = ["member_id", "week_start"]
    )],
    indexes = [Index(name = "idx_weekly_report_member", columnList = "member_id, week_start DESC")]
)
class WeeklyReport(

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

    /** AI가 준 4섹션 JSON (JSONB) */
    @Column(nullable = false, columnDefinition = "TEXT")
    var sectionsJson: String,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    /** 보호자 미션 섹션 추가 (on-demand 캐시) */
    fun updateSections(newSectionsJson: String) {
        this.sectionsJson = newSectionsJson
        this.updatedAt = LocalDateTime.now()
    }
}
