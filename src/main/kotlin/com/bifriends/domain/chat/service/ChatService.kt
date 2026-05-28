package com.bifriends.domain.chat.service

import com.bifriends.domain.chat.dto.ChatMessageRequest
import com.bifriends.domain.chat.dto.ChatMessageResponse
import com.bifriends.infrastructure.ai.AiChatClient
import com.bifriends.infrastructure.ai.dto.AiChatRequest
import org.springframework.stereotype.Service

@Service
class ChatService(
    private val aiChatClient: AiChatClient,
) {

    /**
     * FE에서 받은 프로필·메시지를 그대로 AI 요청 body에 실어 전달한다.
     * [memberId]만 JWT에서 채우고 DB 조회는 하지 않는다.
     */
    fun sendMessage(memberId: Long, request: ChatMessageRequest): ChatMessageResponse {
        val aiRequest = AiChatRequest(
            memberId = memberId,
            nickname = request.nickname,
            grade = request.grade,
            interests = request.interests,
            sessionId = request.sessionId,
            message = request.message,
        )
        val aiResponse = aiChatClient.sendChat(aiRequest)
        return ChatMessageResponse(
            sessionId = request.sessionId,
            reply = aiResponse.reply,
        )
    }
}
