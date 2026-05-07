package com.bifriends.domain.home.model

import com.bifriends.domain.member.model.Member
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 오늘의 할 일
 *
 * ┌──────────────────────────────────────────────────────────────┐
 * │ To do list 삭제 없이 assignedDate 컬럼으로 날짜 구분            │
 * │                                                              │
 * │ - 매일 스케줄러가 오늘 날짜로 3개 생성 (source = SYSTEM)       │
 * │ - Agent가 추가 생성 가능 (source = AGENT, 최대 2개 추가)       │
 * │ - 과거 데이터가 남아 성장일기 등 이력 기능에 활용 가능           │
 * └──────────────────────────────────────────────────────────────┘
 *
 * 제약 조건 (기능명세 HOM-08-09)
 * - 하루 최대 5개 (SYSTEM 3 + AGENT 2)
 * - SYSTEM todo는 삭제/수정 불가 (MVP 기준)
 * - AGENT todo만 수정/삭제 가능
 */
@Entity
@Table(
    name = "todos",
    indexes = [
        // 홈 화면 진입 시 가장 많이 쓰는 조회: member + 날짜
        Index(name = "idx_todos_member_date", columnList = "member_id, assigned_date"),
    ]
)
class Todo(
     
    //PK, 자동 생성
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    // 할 일 소유자 (회원)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    /** 할 일 유형 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: TodoType,

    /** 할 일 제목 */
    @Column(nullable = false)
    var title: String,

    /** 예상 소요 시간 (초) */
    @Column(nullable = false)
    val estimatedTimeSec: Int,

    /** 완료 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TodoStatus = TodoStatus.PENDING,

    /**
     * 생성 출처
     * SYSTEM → 스케줄러 자동 생성, 삭제/수정 불가
     * AGENT  → AI Agent 생성, 수정/삭제 가능
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val source: TodoSource = TodoSource.SYSTEM,

    /**
     * 학습 연결 과목 (LEARNING 타입에만 사용)
     * MATH     → 수학 공부방으로 이동
     * LANGUAGE → 국어 공부방으로 이동
     * null     → 일요일: 전체 공부방 목록으로 이동
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val learningType: LearningType? = null,

    /**
     * 할당 날짜 (KST 기준 LocalDate)
     * B안 핵심 컬럼: 이 값으로 오늘의 할 일을 조회하고,
     * 과거 이력도 날짜별로 조회할 수 있다.
     */
    @Column(nullable = false)
    val assignedDate: LocalDate,

    /** 완료 처리 시각 */
    @Column
    var completedAt: LocalDateTime? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {

    // ── 도메인 메서드 ──────────────────────────────────────────────────────

    /**
     * 할 일을 완료 처리한다.
     * @throws IllegalStateException 이미 완료된 경우
     */
    fun complete() {
        check(status == TodoStatus.PENDING) { "이미 완료된 할 일입니다. id=$id" }
        status = TodoStatus.COMPLETED
        completedAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    /**
     * Agent todo의 제목을 수정한다.
     * @throws IllegalStateException SYSTEM todo에 호출된 경우
     */
    fun updateTitle(newTitle: String) {
        check(source == TodoSource.AGENT) { "시스템 할 일은 수정할 수 없습니다. id=$id" }
        title = newTitle
        updatedAt = LocalDateTime.now()
    }

    /** SYSTEM 생성 할 일 여부 */
    fun isSystemTodo(): Boolean = source == TodoSource.SYSTEM
}
