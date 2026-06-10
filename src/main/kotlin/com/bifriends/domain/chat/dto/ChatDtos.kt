package com.bifriends.domain.chat.dto

import com.bifriends.domain.chat.model.ChatMessage
import com.bifriends.domain.chat.model.ChatSession
import com.bifriends.domain.chat.model.MessageRole
import com.bifriends.domain.chat.model.SessionStatus
import com.bifriends.domain.onboarding.model.Interest
import com.fasterxml.jackson.databind.JsonNode
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

// ── FE — 세션 목록 조회 ───────────────────────────────────────────────────────

data class ChatSessionListResponse(
    val sessions: List<ChatSessionSummary>,
)

data class ChatSessionSummary(
    val sessionId: String,
    val title: String?,
    val status: SessionStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(session: com.bifriends.domain.chat.model.ChatSession) = ChatSessionSummary(
            sessionId = session.sessionKey,
            title = session.title,
            status = session.status,
            createdAt = session.createdAt,
            updatedAt = session.updatedAt,
        )
    }
}

// ── FE ↔ BE ──────────────────────────────────────────────────────────────────

/** FE → BE 채팅 메시지 전송 (프로필은 FE가 /members/me 등에서 이미 보유한 값) */
data class ChatMessageRequest(
    @field:NotBlank
    val sessionId: String,
    @field:NotBlank
    val message: String,
    @field:NotBlank
    val nickname: String,
    @field:Min(3)
    @field:Max(6)
    val grade: Int,
    @field:NotEmpty
    @field:Size(max = 3)
    val interests: List<Interest>,
)

data class ChatMessageResponse(
    val sessionId: String,
    /** Leo의 텍스트 응답 */
    val reply: String?,
    /**
     * 앱 내 이동/액션 힌트 — AI 팀 구조 확정 전 JsonNode로 그대로 전달.
     * 예) { "type": "NAVIGATE", "target": "MATH_STUDY", "stepId": 3 }
     */
    val cta: JsonNode? = null,
    /**
     * 이번 응답에서 Leo가 생성한 Todo ID 목록.
     * FE는 이 값을 받으면 홈 화면 할 일 목록을 갱신한다.
     */
    val todosCreated: List<Long>? = null,
)

// ── Leo 내부 API — 세션 메시지 목록 (3.5) ──────────────────────────────────────

data class ChatSessionMessagesResponse(
    val sessionId: String,
    val memberId: Long,
    val messages: List<ChatMessageItem>,
) {
    companion object {
        fun from(session: ChatSession, messages: List<ChatMessage>) = ChatSessionMessagesResponse(
            sessionId = session.sessionKey,
            memberId = session.member.id,
            messages = messages.map { ChatMessageItem.from(it) },
        )
    }
}

data class ChatMessageItem(
    val id: Long,
    val role: MessageRole,
    val content: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(msg: ChatMessage) = ChatMessageItem(
            id = msg.id,
            role = msg.role,
            content = msg.content,
            createdAt = msg.createdAt,
        )
    }
}

// ── Leo 내부 API — 기간별 메시지 조회 (3.6) ────────────────────────────────────

data class ChatMessagesRangeResponse(
    val memberId: Long,
    val from: LocalDateTime,
    val to: LocalDateTime,
    val messages: List<ChatMessageWithSessionItem>,
)

data class ChatMessageWithSessionItem(
    val id: Long,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(msg: ChatMessage) = ChatMessageWithSessionItem(
            id = msg.id,
            sessionId = msg.session.sessionKey,
            role = msg.role,
            content = msg.content,
            createdAt = msg.createdAt,
        )
    }
}

// ── Leo 내부 API — 세션 수정 (3.9) ────────────────────────────────────────────

data class PatchChatSessionRequest(
    val title: String?,
    val status: SessionStatus?,
)

data class PatchChatSessionResponse(
    val sessionId: String,
    val memberId: Long,
    val title: String?,
    val status: SessionStatus,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(session: ChatSession) = PatchChatSessionResponse(
            sessionId = session.sessionKey,
            memberId = session.member.id,
            title = session.title,
            status = session.status,
            updatedAt = session.updatedAt,
        )
    }
}
