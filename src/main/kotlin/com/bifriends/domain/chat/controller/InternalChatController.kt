package com.bifriends.domain.chat.controller

import com.bifriends.domain.chat.dto.ChatMessagesRangeResponse
import com.bifriends.domain.chat.dto.ChatSessionMessagesResponse
import com.bifriends.domain.chat.dto.PatchChatSessionRequest
import com.bifriends.domain.chat.dto.PatchChatSessionResponse
import com.bifriends.domain.chat.service.ChatService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * Leo(AI) 전용 채팅 조회·수정 API.
 * 인증: X-Internal-Service 헤더 (JWT 없음)
 */
@RestController
@RequestMapping("/api/v1/chat")
class InternalChatController(
    private val chatService: ChatService,
) {

    /** Leo 3.5 — 세션 내 메시지 전체 목록 */
    @GetMapping("/sessions/{sessionId}/messages")
    fun getSessionMessages(
        @PathVariable sessionId: String,
    ): ResponseEntity<ChatSessionMessagesResponse> {
        return ResponseEntity.ok(chatService.getSessionMessages(sessionId))
    }

    /**
     * Leo 3.6 — 회원 기간별 메시지 조회
     *
     * Query params: memberId, from (ISO-8601), to (ISO-8601)
     * 예) ?memberId=1&from=2026-05-25T00:00:00&to=2026-05-31T23:59:59
     */
    @GetMapping("/messages")
    fun getMessagesByRange(
        @RequestParam memberId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime,
    ): ResponseEntity<ChatMessagesRangeResponse> {
        return ResponseEntity.ok(chatService.getMessagesByRange(memberId, from, to))
    }

    /** Leo 3.9 — 세션 제목·상태 수정 */
    @PatchMapping("/sessions/{sessionId}")
    fun patchSession(
        @PathVariable sessionId: String,
        @RequestBody request: PatchChatSessionRequest,
    ): ResponseEntity<PatchChatSessionResponse> {
        return ResponseEntity.ok(chatService.patchSession(sessionId, request))
    }
}
