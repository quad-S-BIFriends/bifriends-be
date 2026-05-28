package com.bifriends.infrastructure.ai.dto

import com.bifriends.domain.onboarding.model.Interest
import com.fasterxml.jackson.annotation.JsonProperty

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

/** AI → BE 채팅 응답 (명세 확정 전 최소 필드) */
data class AiChatResponse(
    val reply: String? = null,
)
