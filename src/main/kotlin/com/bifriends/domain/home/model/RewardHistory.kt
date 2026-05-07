package com.bifriends.domain.home.model

import com.bifriends.domain.member.model.Member
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 풀 보상 이력
 *
 * 언제, 어떤 행동으로, 얼마의 풀을 획득했는지 기록한다.
 * - 감사 로그 / 디버깅 / 향후 통계 분석 용도
 * - 이 테이블은 append-only 로 관리하며 수정하지 않는다.
 */
@Entity
@Table(
    name = "reward_history",
    indexes = [
        Index(name = "idx_reward_history_member_id", columnList = "member_id, created_at")
    ]
)
class RewardHistory(
     
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    /** 보상 출처 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val source: RewardSource,

    /** 획득 풀 양 */
    @Column(nullable = false)
    val amount: Int,

    /**
     * 보상과 연관된 엔티티 ID (선택)
     * 예) todo 완료 → todo.id, 문제 정답 → question.id
     */
    @Column
    val refId: Long? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
