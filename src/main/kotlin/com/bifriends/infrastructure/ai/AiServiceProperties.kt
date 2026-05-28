package com.bifriends.infrastructure.ai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ai.service")
data class AiServiceProperties(
    val enabled: Boolean = false,
    val baseUrl: String = "http://bifriends-ai:8000",
    /** AI 채팅 엔드포인트 경로 (base-url 기준). 명세 확정 후 수정 */
    val chatPath: String = "/v1/chat",
)
