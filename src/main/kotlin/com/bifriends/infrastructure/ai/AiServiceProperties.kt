package com.bifriends.infrastructure.ai

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ai.service")
data class AiServiceProperties(
    val enabled: Boolean = false,
    val baseUrl: String = "http://bifriends-ai:8000",
    /** AI 채팅 엔드포인트 경로 */
    val chatPath: String = "/api/v1/ai/chat",
    /** BE 스케줄러 → AI 주간 안전 보고서 배치 트리거 경로 */
    val batchWeeklySafetyPath: String = "/api/v1/ai/batch/weekly-safety",
    /** BE 스케줄러 → AI 주간 성장 리포트(부모용) 배치 트리거 경로 */
    val batchWeeklyReportPath: String = "/api/v1/ai/report/weekly",
    /** 친구랑 감정 학습 시나리오 생성 요청 경로 (EMO-04) */
    val emotionScenarioPath: String = "/api/v1/ai/content/scenario",
)
