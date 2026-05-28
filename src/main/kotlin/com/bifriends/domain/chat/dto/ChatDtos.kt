package com.bifriends.domain.chat.dto

import com.bifriends.domain.onboarding.model.Interest
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

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
    val reply: String?,
)
