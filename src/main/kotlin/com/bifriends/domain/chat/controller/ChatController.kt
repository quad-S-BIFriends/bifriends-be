package com.bifriends.domain.chat.controller

import com.bifriends.domain.chat.dto.ChatMessageRequest
import com.bifriends.domain.chat.dto.ChatMessageResponse
import com.bifriends.domain.chat.dto.ChatSessionListResponse
import com.bifriends.domain.chat.dto.ChatSessionMessagesResponse
import com.bifriends.domain.chat.service.ChatService
import com.bifriends.infrastructure.security.JwtProvider
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    private val chatService: ChatService,
    private val jwtProvider: JwtProvider,
) {

    /** FE 채팅 메시지 → BE가 AI로 중계 (JWT 인증) */
    @PostMapping("/messages")
    fun postMessage(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody request: ChatMessageRequest,
    ): ResponseEntity<ChatMessageResponse> {
        val memberId = jwtProvider.getMemberId(token.removePrefix("Bearer "))
        return ResponseEntity.ok(chatService.sendMessage(memberId, request))
    }

    /** 내 채팅 세션 목록 — 최근 활동 순 */
    @GetMapping("/sessions")
    fun getMySessions(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<ChatSessionListResponse> {
        val memberId = jwtProvider.getMemberId(token.removePrefix("Bearer "))
        return ResponseEntity.ok(chatService.getMySessions(memberId))
    }

    /** 세션 상세 — 메시지 전체 목록 (오래된 순) */
    @GetMapping("/sessions/{sessionId}")
    fun getMySessionMessages(
        @RequestHeader("Authorization") token: String,
        @PathVariable sessionId: String,
    ): ResponseEntity<ChatSessionMessagesResponse> {
        val memberId = jwtProvider.getMemberId(token.removePrefix("Bearer "))
        return ResponseEntity.ok(chatService.getMySessionMessages(memberId, sessionId))
    }

    /** 세션 삭제 — 메시지 포함 전체 삭제 */
    @DeleteMapping("/sessions/{sessionId}")
    fun deleteSession(
        @RequestHeader("Authorization") token: String,
        @PathVariable sessionId: String,
    ): ResponseEntity<Void> {
        val memberId = jwtProvider.getMemberId(token.removePrefix("Bearer "))
        chatService.deleteSession(memberId, sessionId)
        return ResponseEntity.noContent().build()
    }
}
