package com.bifriends.domain.home.service

import com.bifriends.domain.home.dto.AgentTodoCreateRequest
import com.bifriends.domain.home.dto.AgentTodoUpdateRequest
import com.bifriends.domain.home.dto.TodoCompleteResult
import com.bifriends.domain.home.dto.TodoResponse
import com.bifriends.domain.home.model.*
import com.bifriends.domain.home.repository.TodoRepository
import com.bifriends.domain.member.model.Member
import com.bifriends.domain.member.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Service
@Transactional(readOnly = true)
class TodoService(
    private val todoRepository: TodoRepository,
    private val memberRepository: MemberRepository,
    private val userStatsService: UserStatsService,
) {

    companion object {
        private val KST = ZoneId.of("Asia/Seoul")
        const val MAX_TODOS_PER_DAY = 5       // 시스템 3 + Agent 추가 최대 2
        const val SYSTEM_TODOS_PER_DAY = 3

        /** 기본 할 일 정의 (기능명세 HOM-08-09) */
        private data class DefaultTodo(
            val type: TodoType,
            val title: String,
            val estimatedTimeSec: Int,
        )

        private val DEFAULT_TODOS = listOf(
            DefaultTodo(TodoType.CHAT,     "레오랑 이야기하기 🗣️",  60),
            DefaultTodo(TodoType.LEARNING, "오늘의 문제 3개 풀기 📚", 180),
            DefaultTodo(TodoType.EMOTION,  "친구 기분 알아보기 💌",  120),
        )
    }

    // ── 오늘의 할 일 조회 ─────────────────────────────────────────────────

    fun getTodayTodos(memberId: Long): List<TodoResponse> {
        val today = LocalDate.now(KST)
        return todoRepository
            .findByMemberIdAndAssignedDateOrderByCreatedAtAsc(memberId, today)
            .map { TodoResponse.from(it) }
    }

    // ── 스케줄러용: 일일 할 일 자동 생성 ──────────────────────────────────

    /**
     * 특정 회원의 오늘 할 일 3개를 생성한다.
     * 이미 생성된 경우 스킵 (멱등 처리 — 스케줄러 중복 실행 방지).
     */
    @Transactional
    fun generateDailyTodos(member: Member, today: LocalDate) {
        // 이미 오늘 할 일이 있으면 스킵
        if (todoRepository.existsByMemberIdAndAssignedDate(member.id, today)) return

        val learningType = LearningTypePolicy.forDate(today)

        val todos = DEFAULT_TODOS.map { default ->
            Todo(
                member = member,
                type = default.type,
                title = default.title,
                estimatedTimeSec = default.estimatedTimeSec,
                source = TodoSource.SYSTEM,
                learningType = if (default.type == TodoType.LEARNING) learningType else null,
                assignedDate = today,
            )
        }
        todoRepository.saveAll(todos)
    }

    // ── 할 일 완료 처리 ───────────────────────────────────────────────────

    /**
     * 할 일을 완료 처리하고 보상을 지급한다.
     *
     * 보상 규칙:
     * - 단일 완료: +1풀
     * - 오늘 할 일 전부 완료 시: +3풀 보너스 추가
     *
     * 모든 처리는 단일 트랜잭션으로 묶인다.
     */
    @Transactional
    fun completeTodo(memberId: Long, todoId: Long): TodoCompleteResult {
        val todo = todoRepository.findById(todoId)
            .orElseThrow { IllegalArgumentException("할 일을 찾을 수 없습니다. id=$todoId") }

        check(todo.member.id == memberId) { "본인의 할 일만 완료할 수 있습니다." }

        // 엔티티 도메인 메서드 호출 (이미 완료면 IllegalStateException)
        todo.complete()

        // 단일 완료 보상 지급
        val singleReward = userStatsService.earnReward(
            memberId = memberId,
            source = RewardSource.TODO_SINGLE,
            amount = RewardPolicy.TODO_SINGLE,
            refId = todoId,
        )

        // 전체 완료 보너스 체크
        val today = LocalDate.now(KST)
        val remainingPending = todoRepository.countByMemberIdAndAssignedDateAndStatus(
            memberId, today, TodoStatus.PENDING
        )
        val allCompleteBonus = if (remainingPending == 0) {
            userStatsService.earnReward(
                memberId = memberId,
                source = RewardSource.TODO_ALL_COMPLETE,
                amount = RewardPolicy.TODO_ALL_COMPLETE,
            )
        } else null

        return TodoCompleteResult(
            todo = TodoResponse.from(todo),
            singleReward = singleReward,
            allCompleteBonus = allCompleteBonus,
        )
    }

    // ── Agent용 CRUD ──────────────────────────────────────────────────────

    /**
     * Agent가 할 일을 추가한다.
     * 하루 최대 5개(SYSTEM 3 + AGENT 2) 제한.
     */
    @Transactional
    fun createAgentTodo(memberId: Long, request: AgentTodoCreateRequest): TodoResponse {
        val member = memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

        val today = LocalDate.now(KST)
        val currentCount = todoRepository.countByMemberIdAndAssignedDate(memberId, today)
        check(currentCount < MAX_TODOS_PER_DAY) {
            "하루 최대 할 일 개수($MAX_TODOS_PER_DAY)를 초과했습니다."
        }

        val todo = todoRepository.save(
            Todo(
                member = member,
                type = TodoType.CUSTOM,
                title = request.title,
                estimatedTimeSec = request.estimatedTimeSec ?: 0,
                source = TodoSource.AGENT,
                assignedDate = today,
            )
        )
        return TodoResponse.from(todo)
    }

    /**
     * Agent가 생성한 할 일의 제목을 수정한다.
     * SYSTEM 할 일에 호출하면 예외 발생.
     */
    @Transactional
    fun updateAgentTodo(memberId: Long, todoId: Long, request: AgentTodoUpdateRequest): TodoResponse {
        val todo = findTodoOfMember(memberId, todoId)
        todo.updateTitle(request.title)  // 엔티티에서 SYSTEM 여부 검증
        return TodoResponse.from(todo)
    }

    /**
     * Agent가 생성한 할 일을 삭제한다.
     * SYSTEM 할 일 삭제 시도 시 403 처리를 위해 예외 발생.
     */
    @Transactional
    fun deleteAgentTodo(memberId: Long, todoId: Long) {
        val todo = findTodoOfMember(memberId, todoId)
        check(todo.source == TodoSource.AGENT) {
            "시스템 할 일은 삭제할 수 없습니다. id=$todoId"
        }
        todoRepository.delete(todo)
    }

    // ── 스케줄러 지원 ────────────────────────────────────────────────────

    /** 특정 날짜의 할 일이 이미 생성됐는지 확인 (스케줄러 로깅용) */
    fun isTodayTodoGenerated(memberId: Long, date: LocalDate): Boolean =
        todoRepository.existsByMemberIdAndAssignedDate(memberId, date)

    // ── 내부 유틸 ────────────────────────────────────────────────────────

    private fun findTodoOfMember(memberId: Long, todoId: Long): Todo {
        val todo = todoRepository.findById(todoId)
            .orElseThrow { IllegalArgumentException("할 일을 찾을 수 없습니다. id=$todoId") }
        check(todo.member.id == memberId) { "본인의 할 일만 수정/삭제할 수 있습니다." }
        return todo
    }
}
