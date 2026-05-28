package com.bifriends.domain.learning.model

import com.bifriends.domain.member.model.Member
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_korean_progress",
    uniqueConstraints = [UniqueConstraint(name = "uq_user_korean_progress", columnNames = ["member_id", "korean_step_id"])]
)
class UserKoreanProgress(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "korean_step_id", nullable = false)
    val koreanStep: KoreanStep,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_korean_progress_cycles",
        joinColumns = [JoinColumn(name = "progress_id")]
    )
    @Column(name = "cycle_number")
    val completedCycles: MutableSet<Int> = mutableSetOf(),

    @Column(nullable = false)
    var isStepCompleted: Boolean = false,

    @Column
    var lastAccessedAt: LocalDateTime? = null,
)
