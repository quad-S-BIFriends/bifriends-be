package com.bifriends.infrastructure.ai

import com.bifriends.infrastructure.ai.dto.AiBatchWeeklySafetyRequest
import com.bifriends.infrastructure.ai.dto.AiBatchWeeklySafetyResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * BE → AI 배치 트리거 클라이언트.
 * 채팅과 달리 배치는 실패해도 앱 기능에 즉각 영향을 주지 않으므로
 * 예외를 삼키고 로그만 남긴다.
 */
@Component
class AiBatchClient(
    private val restClient: RestClient,
    private val properties: AiServiceProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * AI 주간 안전 보고서 배치를 트리거한다.
     * [properties.enabled]가 false이면 호출하지 않는다 (로컬 개발 환경).
     *
     * @return 성공 여부
     */
    fun triggerWeeklySafety(request: AiBatchWeeklySafetyRequest): Boolean {
        if (!properties.enabled) {
            log.info("[AiBatchClient] AI 연동 비활성화 — 배치 트리거 스킵 (targetDate={})", request.targetDate)
            return false
        }

        return try {
            restClient.post()
                .uri(properties.batchWeeklySafetyPath)
                .body(request)
                .retrieve()
                .body(AiBatchWeeklySafetyResponse::class.java)
            log.info("[AiBatchClient] 주간 안전 보고서 배치 트리거 성공 (targetDate={})", request.targetDate)
            true
        } catch (e: Exception) {
            log.error("[AiBatchClient] 주간 안전 보고서 배치 트리거 실패 (targetDate={})", request.targetDate, e)
            false
        }
    }
}
