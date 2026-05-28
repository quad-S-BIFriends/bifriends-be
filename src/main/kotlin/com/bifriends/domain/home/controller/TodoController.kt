package com.bifriends.domain.home.controller

import com.bifriends.domain.home.dto.TodoCompleteResult
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

    /** 할 일 완료 처리 — 단일 +1풀, 전부 완료 시 +3풀 보너스 */
    @PatchMapping("/{todoId}/complete")
    fun completeTodo(
        @RequestHeader("Authorization") token: String,
        @PathVariable todoId: Long,
    ): ResponseEntity<TodoCompleteResult> {
        val memberId = extractMemberId(token)
        return ResponseEntity.ok(todoService.completeTodo(memberId, todoId))
    }

    private fun extractMemberId(token: String): Long {
        return jwtProvider.getMemberId(token.removePrefix("Bearer "))
    }
}
