package com.bifriends.infrastructure.ai

import com.bifriends.infrastructure.ai.dto.AiChatRequest
import com.bifriends.infrastructure.ai.dto.AiChatResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class AiChatClient(
    private val restClient: RestClient,
    private val properties: AiServiceProperties,
) {

    fun sendChat(request: AiChatRequest): AiChatResponse {
        if (!properties.enabled) {
            return AiChatResponse(reply = null)
        }
        return restClient.post()
            .uri(properties.chatPath)
            .body(request)
            .retrieve()
            .body(AiChatResponse::class.java)
            ?: throw IllegalStateException("AI 채팅 응답이 비어 있습니다.")
    }
}
