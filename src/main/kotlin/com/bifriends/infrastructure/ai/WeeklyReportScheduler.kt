package com.bifriends.infrastructure.ai

import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.domain.report.service.ReportService
import com.bifriends.infrastructure.ai.dto.AiBatchWeeklyReportRequest
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 주간 성장 리포트(부모용) 배치 스케줄러
 *
 * 매주 월요일 01:00 KST에 온보딩 완료 회원 각각에 대해
 * AI 서버의 주간 리포트 엔드포인트를 1인당 1건씩 호출한다.
 * AI는 member_id + week_start/end 로 집계·LLM 후
 * POST /api/v1/weekly-report 로 결과를 콜백한다.
 *
 * 흐름: BE 스케줄러 → POST /api/v1/ai/report/weekly (1인당 1건, AI 서버)
 *       AI 처리 완료 → POST /api/v1/weekly-report (BE 콜백)
 */
@Component
class WeeklyReportScheduler(
    private val aiBatchClient: AiBatchClient,
    private val memberRepository: MemberRepository,
    private val reportService: ReportService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 매주 월요일 01:00 KST */
    @Scheduled(cron = "0 0 1 * * MON", zone = "Asia/Seoul")
    fun triggerWeeklyGrowthReport() {
        val (weekStart, weekEnd) = WeeklyBatchWeekRange.previousCompletedWeek()

        val members = memberRepository.findAllByOnboardingCompletedTrue()
        log.info(
            "[WeeklyReportScheduler] 주간 성장 리포트 배치 시작 — weekStart={}, weekEnd={}, 대상 회원 수={}",
            weekStart, weekEnd, members.size,
        )

        var successCount = 0
        var failCount = 0

        for (member in members) {
            val learningSummary = try {
                reportService.getLearningSummary(member.id, weekStart, weekEnd)
            } catch (e: Exception) {
                log.error(
                    "[WeeklyReportScheduler] 학습 요약 조회 실패 — memberId={}, weekStart={}",
                    member.id, weekStart, e,
                )
                failCount++
                continue
            }

            val success = aiBatchClient.triggerWeeklyReport(
                AiBatchWeeklyReportRequest(
                    memberId = member.id,
                    weekStart = weekStart,
                    weekEnd = weekEnd,
                    grade = member.grade,
                    learningSummary = learningSummary,
                ),
            )
            if (success) successCount++ else failCount++
        }

        log.info(
            "[WeeklyReportScheduler] 배치 트리거 완료 — 성공={}, 실패={}, weekStart={}",
            successCount, failCount, weekStart,
        )
    }
}
