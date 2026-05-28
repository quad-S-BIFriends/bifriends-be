package com.bifriends.domain.home.dto

import com.bifriends.domain.home.model.LearningType
import com.bifriends.domain.home.model.Todo
import com.bifriends.domain.home.model.TodoSource
import com.bifriends.domain.home.model.TodoStatus
import com.bifriends.domain.home.model.TodoType
import java.time.LocalDate

// ── 조회 응답 ─────────────────────────────────────────────────────────────

data class TodoResponse(
    val id: Long,
    val type: TodoType,
    val title: String,
    val status: TodoStatus,
    val estimatedTimeSec: Int,
    val source: TodoSource,
    val learningType: LearningType?,
    val assignedDate: LocalDate,
) {
    companion object {
        fun from(todo: Todo) = TodoResponse(
            id = todo.id,
            type = todo.type,
            title = todo.title,
            status = todo.status,
            estimatedTimeSec = todo.estimatedTimeSec,
            source = todo.source,
            learningType = todo.learningType,
            assignedDate = todo.assignedDate,
        )
    }
}

// ── 완료 처리 결과 ────────────────────────────────────────────────────────

data class TodoCompleteResult(
    /** 완료된 할 일 */
    val todo: TodoResponse,
    /** 단일 완료 보상 (+1풀) */
    val singleReward: RewardResult,
    /** 전체 완료 보너스 (+3풀), 아직 남은 할 일이 있으면 null */
    val allCompleteBonus: RewardResult?,
) {
    val leveledUp: Boolean
        get() = singleReward.leveledUp || (allCompleteBonus?.leveledUp == true)
}

// ── Agent CRUD 요청 (Leo 내부 API용 — X-Internal-Service 헤더) ──────────────

data class AgentTodoCreateRequest(
    /** 할 일을 생성할 회원 ID (Leo가 채팅 세션에서 알고 있는 값) */
    val memberId: Long,
    val title: String,
    val estimatedTimeSec: Int? = null,
)

data class AgentTodoUpdateRequest(
    /** 수정 대상 회원 ID (소유권 검증용) */
    val memberId: Long,
    val title: String,
)
