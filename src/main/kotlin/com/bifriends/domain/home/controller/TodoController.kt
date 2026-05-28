package com.bifriends.domain.home.controller

import com.bifriends.domain.home.dto.AgentTodoCreateRequest
import com.bifriends.domain.home.dto.AgentTodoUpdateRequest
import com.bifriends.domain.home.dto.TodoCompleteResult
import com.bifriends.domain.home.dto.TodoResponse
import com.bifriends.domain.home.service.TodoService
import com.bifriends.infrastructure.security.JwtProvider
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/todos")
class TodoController(
    private val todoService: TodoService,
    private val jwtProvider: JwtProvider,
) {

    // ── 앱 클라이언트 (JWT 인증) ──────────────────────────────────────────────

    /** 할 일 완료 처리 — 단일 +1풀, 전부 완료 시 +3풀 보너스 */
    @PatchMapping("/{todoId}/complete")
    fun completeTodo(
        @RequestHeader("Authorization") token: String,
        @PathVariable todoId: Long,
    ): ResponseEntity<TodoCompleteResult> {
        val memberId = extractMemberId(token)
        return ResponseEntity.ok(todoService.completeTodo(memberId, todoId))
    }

    // ── Leo 내부 API (X-Internal-Service 인증) ────────────────────────────────

    /**
     * Leo가 회원의 할 일을 추가한다 (하루 최대 5개).
     * body에 memberId 포함 필수.
     */
    @PostMapping
    fun createAgentTodo(
        @RequestBody request: AgentTodoCreateRequest,
    ): ResponseEntity<TodoResponse> {
        return ResponseEntity.ok(todoService.createAgentTodo(request))
    }

    /** Leo가 생성한 할 일의 제목을 수정한다. SYSTEM 할 일에 호출하면 400. */
    @PatchMapping("/{todoId}")
    fun updateAgentTodo(
        @PathVariable todoId: Long,
        @RequestBody request: AgentTodoUpdateRequest,
    ): ResponseEntity<TodoResponse> {
        return ResponseEntity.ok(todoService.updateAgentTodo(todoId, request))
    }

    /** Leo가 생성한 할 일을 삭제한다. SYSTEM 할 일에 호출하면 400. */
    @DeleteMapping("/{todoId}")
    fun deleteAgentTodo(
        @PathVariable todoId: Long,
        @RequestParam memberId: Long,
    ): ResponseEntity<Void> {
        todoService.deleteAgentTodo(memberId, todoId)
        return ResponseEntity.noContent().build()
    }

    private fun extractMemberId(token: String): Long {
        return jwtProvider.getMemberId(token.removePrefix("Bearer "))
    }
}
