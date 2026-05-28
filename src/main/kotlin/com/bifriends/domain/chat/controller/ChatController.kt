package com.bifriends.domain.chat.controller

import com.bifriends.domain.chat.dto.ChatMessageRequest
import com.bifriends.domain.chat.dto.ChatMessageResponse
import com.bifriends.domain.chat.service.ChatService
import com.bifriends.infrastructure.security.JwtProvider
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
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
}
