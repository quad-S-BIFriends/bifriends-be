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
 *   "growth_summary": "...",
 *   "math":           { "well_done": "...", "struggled": "..." },
 *   "korean":         { "well_done": "...", "struggled": "..." },
 *   "parent_mission": { "praise": "...", "activity": "..." }
 * }
 *
 * parent_mission은 weekly 콜백 시 저장되며, 부모가 '미션 받기'를 누르면 [missionRevealed]가 true가 된다.
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

    /** 부모가 '미션 받기'를 눌러 미션을 수령했는지 */
    @Column(nullable = false)
    var missionRevealed: Boolean = false,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun updateSections(newSectionsJson: String) {
        this.sectionsJson = newSectionsJson
        this.missionRevealed = false
        this.updatedAt = LocalDateTime.now()
    }

    fun revealMission() {
        this.missionRevealed = true
        this.updatedAt = LocalDateTime.now()
    }
}
