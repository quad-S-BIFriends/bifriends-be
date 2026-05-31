package com.bifriends.infrastructure.ai

import com.bifriends.infrastructure.ai.dto.AiBatchWeeklySafetyRequest
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

/**
 * 주간 안전 보고서 배치 스케줄러
 *
 * 매주 금요일 18:00 KST에 AI 서버의 배치 엔드포인트를 트리거한다.
 * AI는 해당 주의 채팅 내용을 분석해 보호자 주간 안전 보고서를 생성한다.
 *
 * 흐름: BE 스케줄러 → POST /api/v1/ai/batch/weekly-safety (AI 서버)
 */
@Component
class WeeklySafetyScheduler(
    private val aiBatchClient: AiBatchClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val KST = ZoneId.of("Asia/Seoul")

    /**
     * 매주 금요일 18:00 KST 실행
     * cron = "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 18 * * FRI", zone = "Asia/Seoul")
    fun triggerWeeklySafetyReport() {
        val today = LocalDate.now(KST)
        log.info("[WeeklySafetyScheduler] 주간 안전 보고서 배치 시작 — targetDate={}", today)

        val success = aiBatchClient.triggerWeeklySafety(
            AiBatchWeeklySafetyRequest(targetDate = today)
        )

        if (success) {
            log.info("[WeeklySafetyScheduler] 배치 트리거 완료 — targetDate={}", today)
        } else {
            log.warn("[WeeklySafetyScheduler] 배치 트리거 실패 또는 스킵 — targetDate={}", today)
        }
    }
}
