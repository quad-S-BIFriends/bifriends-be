package com.bifriends.domain.learning.model

import com.bifriends.domain.member.model.Member
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 문제 풀이 시도 기록
 *
 * - 오답 시마다 attempts +1, 최종 정답 시 solved = true 로 확정
 * - hints_used: FE가 노출한 힌트 단계 수 (0~3), 정답 시 최종값 전달
 * - solved_at: 주간 범위 필터용
 *
 * (member_id, step_id, cycle_number, question_index) 조합이 문제 한 개를 식별한다.
 */
@Entity
@Table(
    name = "learning_attempt",
    indexes = [
        Index(name = "idx_learning_attempt_member_subject", columnList = "member_id, subject, solved_at"),
        Index(name = "idx_learning_attempt_problem", columnList = "member_id, step_id, cycle_number, question_index"),
    ]
)
class LearningAttempt(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val subject: LearningSubject,

    /** 개념별 집계용 (예: "받아올림 없는 세 자리 덧셈") */
    @Column(nullable = false)
    val concept: String,

    @Column(nullable = false)
    val stepId: Long,

    @Column(nullable = false)
    val cycleNumber: Int,

    @Column(nullable = false)
    val questionIndex: Int,

    /** 정답까지 시도한 횟수 (1이면 한 번에 맞힘) */
    @Column(nullable = false)
    var attempts: Int = 1,

    /** 해당 문제에서 노출된 힌트 단계 수 (0~3) */
    @Column(nullable = false)
    var hintsUsed: Int = 0,

    /** 정답 여부 — 정답 확정 시 true, 오답 진행 중이면 false */
    @Column(nullable = false)
    var solved: Boolean = false,

    /** 정답 처리 시각 (주간 범위 필터용, solved=true 시 설정) */
    @Column
    var solvedAt: LocalDateTime? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun markWrong() {
        attempts++
        updatedAt = LocalDateTime.now()
    }

    fun markSolved(hintsUsed: Int) {
        this.solved = true
        this.hintsUsed = hintsUsed
        this.solvedAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }
}
