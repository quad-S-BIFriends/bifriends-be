package com.bifriends.infrastructure.ai

import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.infrastructure.ai.dto.AiBatchWeeklySafetyRequest
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * 주간 안전 보고서 배치 스케줄러
 *
 * 매주 금요일 18:00 KST에 온보딩 완료 회원 각각에 대해
 * AI 서버의 배치 엔드포인트를 1인당 1건씩 호출한다.
 * AI는 member_id + week_start/end 로 채팅을 분석하고
 * POST /api/v1/weekly-safety-report 로 결과를 콜백한다.
 *
 * 흐름: BE 스케줄러 → POST /api/v1/ai/batch/weekly-safety (1인당 1건, AI 서버)
 *       AI 분석 완료 → POST /api/v1/weekly-safety-report (BE 콜백)
 */
@Component
class WeeklySafetyScheduler(
    private val aiBatchClient: AiBatchClient,
    private val memberRepository: MemberRepository,
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
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = today // 금요일

        val members = memberRepository.findAllByOnboardingCompletedTrue()
        log.info(
            "[WeeklySafetyScheduler] 주간 안전 보고서 배치 시작 — weekStart={}, weekEnd={}, 대상 회원 수={}",
            weekStart, weekEnd, members.size
        )

        var successCount = 0
        var failCount = 0

        for (member in members) {
            val success = aiBatchClient.triggerWeeklySafety(
                AiBatchWeeklySafetyRequest(
                    memberId = member.id,
                    weekStart = weekStart,
                    weekEnd = weekEnd,
                )
            )
            if (success) successCount++ else failCount++
        }

        log.info(
            "[WeeklySafetyScheduler] 배치 트리거 완료 — 성공={}, 실패={}, weekStart={}",
            successCount, failCount, weekStart
        )
    }
}
