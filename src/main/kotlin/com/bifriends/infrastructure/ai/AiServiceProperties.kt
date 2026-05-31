package com.bifriends.infrastructure.ai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ai.service")
data class AiServiceProperties(
    val enabled: Boolean = false,
    val baseUrl: String = "http://bifriends-ai:8000",
    /** AI 채팅 엔드포인트 경로 */
    val chatPath: String = "/v1/chat",
    /** BE 스케줄러 → AI 주간 안전 보고서 배치 트리거 경로 */
    val batchWeeklySafetyPath: String = "/api/v1/ai/batch/weekly-safety",
)
