package com.bifriends.domain.home.service

import com.bifriends.domain.member.repository.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId

/**
 * 할 일 자동 생성 스케줄러
 *
 * 매일 00:00 KST에 전체 회원의 오늘 할 일 3개를 생성한다.
 *
 * ──────────────────────────────────────────────────────────────
 * MVP 한계 및 향후 개선 방안
 * ──────────────────────────────────────────────────────────────
 * 현재: 전체 회원을 한 번에 조회해서 순차 처리
 *       → 회원 수가 적은 MVP 단계에서는 문제 없음
 *
 * 회원 수 증가 시: Spring Batch의 Chunk 처리 방식으로 전환
 *                  (페이징으로 나눠서 처리, 실패한 청크만 재시도)
 * ──────────────────────────────────────────────────────────────
 */
@Component
class TodoScheduler(
    private val todoService: TodoService,
    private val memberRepository: MemberRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 매일 00:00 KST 실행
     * cron = "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    fun generateDailyTodos() {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        log.info("[TodoScheduler] 일일 할 일 생성 시작 — date=$today")

        val members = memberRepository.findAll()
        var successCount = 0
        var skipCount = 0

        members.forEach { member ->
            try {
                // generateDailyTodos 내부에서 이미 생성됐으면 스킵 (멱등)
                val beforeExists = todoService.isTodayTodoGenerated(member.id, today)
                todoService.generateDailyTodos(member, today)
                if (beforeExists) skipCount++ else successCount++
            } catch (e: Exception) {
                log.error("[TodoScheduler] 할 일 생성 실패 — memberId=${member.id}", e)
            }
        }

        log.info("[TodoScheduler] 완료 — 생성=$successCount, 스킵=$skipCount, 전체=${members.size}")
    }
}
