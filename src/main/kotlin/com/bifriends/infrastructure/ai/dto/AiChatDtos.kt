package com.bifriends.infrastructure.ai.dto

import com.bifriends.domain.chat.dto.ChatTodoCreated
import com.bifriends.domain.onboarding.model.Interest
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

/** BE → AI 채팅 요청 (AI 팀 스펙: snake_case) */
data class AiChatRequest(
    @JsonProperty("member_id")
    val memberId: Long,
    val nickname: String,
    val grade: Int,
    val interests: List<Interest>,
    @JsonProperty("session_id")
    val sessionId: String,
    val message: String,
)

/**
 * AI → BE 채팅 응답
 *
 * - [reply]        : Leo의 텍스트 응답
 * - [cta]          : 앱 내 이동/액션 힌트 (구조 AI 팀 확정 전 JsonNode로 수신)
 *                    예) { "type": "NAVIGATE", "target": "MATH_STUDY", "stepId": 3 }
 * - [todosCreated] : Leo가 이번 응답에서 등록한 할 일 요약 (title, assigned_date)
 *                    Agent Todo API로 BE에 이미 생성된 뒤 AI가 메타데이터만 전달
 */
data class AiChatResponse(
    val reply: String? = null,
    val cta: JsonNode? = null,
    @JsonProperty("todos_created")
    val todosCreated: List<ChatTodoCreated>? = null,
)
