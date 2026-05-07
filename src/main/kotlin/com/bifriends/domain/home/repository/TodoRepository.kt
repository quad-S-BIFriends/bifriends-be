package com.bifriends.domain.home.repository

import com.bifriends.domain.home.model.Todo
import com.bifriends.domain.home.model.TodoSource
import com.bifriends.domain.home.model.TodoStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface TodoRepository : JpaRepository<Todo, Long> {

    /** 특정 날짜의 할 일 목록 조회 (홈 화면용) */
    fun findByMemberIdAndAssignedDateOrderByCreatedAtAsc(
        memberId: Long,
        assignedDate: LocalDate,
    ): List<Todo>

    /** 오늘 할 일이 이미 생성됐는지 확인 (스케줄러 중복 실행 방지) */
    fun existsByMemberIdAndAssignedDate(memberId: Long, assignedDate: LocalDate): Boolean

    /** 특정 날짜의 할 일 수 (최대 5개 제한 체크용) */
    fun countByMemberIdAndAssignedDate(memberId: Long, assignedDate: LocalDate): Int

    /**
     * 특정 날짜의 미완료 할 일 수
     * 전체 완료 보너스 지급 시점 판단에 사용
     */
    fun countByMemberIdAndAssignedDateAndStatus(
        memberId: Long,
        assignedDate: LocalDate,
        status: TodoStatus,
    ): Int

    /**
     * 과거 날짜별 할 일 이력 조회 (성장일기 등 이력 기능용)
     * assignedDate 내림차순(최신순)으로 반환
     */
    @Query("""
        SELECT t FROM Todo t
        WHERE t.member.id = :memberId
          AND t.assignedDate BETWEEN :from AND :to
        ORDER BY t.assignedDate DESC, t.createdAt ASC
    """)
    fun findHistoryBetween(memberId: Long, from: LocalDate, to: LocalDate): List<Todo>
}
