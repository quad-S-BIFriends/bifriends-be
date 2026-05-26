package com.bifriends.domain.study.model

import com.bifriends.domain.member.model.Member
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_math_progress",
    uniqueConstraints = [UniqueConstraint(name = "uq_user_math_progress", columnNames = ["member_id", "math_step_id"])]
)
class UserMathProgress(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "math_step_id", nullable = false)
    val mathStep: MathStep,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_math_progress_cycles",
        joinColumns = [JoinColumn(name = "progress_id")]
    )
    @Column(name = "cycle_number")
    val completedCycles: MutableSet<Int> = mutableSetOf(),

    @Column(nullable = false)
    var isStepCompleted: Boolean = false,

    @Column
    var lastAccessedAt: LocalDateTime? = null,
)
