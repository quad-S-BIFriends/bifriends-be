package com.bifriends.infrastructure.ai

import com.bifriends.infrastructure.ai.dto.AiParentMissionRequest
import com.bifriends.infrastructure.ai.dto.AiParentMissionResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * BE → AI 보호자 미션 생성 클라이언트 (RPT-08)
 *
 * [properties.enabled]가 false이면 기본 메시지를 반환한다 (로컬 개발 환경).
 */
@Component
class AiParentMissionClient(
    private val restClient: RestClient,
    private val properties: AiServiceProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generateMission(request: AiParentMissionRequest): AiParentMissionResponse {
        if (!properties.enabled) {
            log.info("[AiParentMissionClient] AI 연동 비활성화 — 기본 미션 반환 (memberId={})", request.memberId)
            return fallbackMission()
        }

        return try {
            restClient.post()
                .uri(properties.parentMissionPath)
                .body(request)
                .retrieve()
                .body(AiParentMissionResponse::class.java)
                ?: fallbackMission()
        } catch (e: Exception) {
            log.error("[AiParentMissionClient] 보호자 미션 생성 실패 (memberId={})", request.memberId, e)
            fallbackMission()
        }
    }

    private fun fallbackMission() = AiParentMissionResponse(
        praise = "이번 주도 열심히 해줬어요! 정말 잘하고 있어요 👏",
        mission = "오늘 아이와 함께 오늘 배운 내용을 5분간 이야기 나눠보세요.",
    )
}
