package com.bifriends.domain.safety.service

import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.domain.report.model.SafetySignal
import com.bifriends.domain.report.model.WeeklyReport
import com.bifriends.domain.report.repository.WeeklyReportRepository
import com.bifriends.domain.safety.dto.SafetyLevel
import com.bifriends.domain.safety.dto.WeeklySafetyReportRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

@Service
class WeeklySafetyService(
    private val weeklyReportRepository: WeeklyReportRepository,
    private val memberRepository: MemberRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun receiveReport(request: WeeklySafetyReportRequest) {
        val member = memberRepository.findById(request.memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=${request.memberId}") }

        // week_end(금요일) 기준으로 week_start(월요일) 계산
        val weekEnd = request.reportDate
        val weekStart = weekEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        // 중복 저장 방지 — 같은 주차 리포트가 이미 있으면 업데이트
        if (weeklyReportRepository.existsByMemberIdAndWeekStart(member.id, weekStart)) {
            log.warn(
                "[WeeklySafety] 이미 저장된 주차 리포트 — memberId={}, weekStart={} (덮어쓰기)",
                member.id, weekStart
            )
            // MVP: 기존 레코드 삭제 후 재저장 (단순 처리)
            weeklyReportRepository.findByMemberIdAndWeekStart(member.id, weekStart)
                ?.let { weeklyReportRepository.delete(it) }
        }

        // safety_signal 파싱 (AI가 신규 필드 미전송 시 safety_level로 fallback)
        val safetySignal = parseSafetySignal(request)
        val safetyScore = request.safetyScore
        val safetyReasonSummary = request.safetyReasonSummary.ifBlank {
            defaultReasonSummary(safetySignal)
        }

        val keywordsJson = if (request.keywords.isNotEmpty())
            objectMapper.writeValueAsString(request.keywords)
        else null

        val report = WeeklyReport(
            member = member,
            weekStart = weekStart,
            weekEnd = weekEnd,
            safetySignal = safetySignal,
            safetyScore = safetyScore,
            safetyReasonSummary = safetyReasonSummary,
            growthSummary = request.growthSummary,
            parentTip = request.parentTip,
            mathSummary = request.mathSummary,
            koreanSummary = request.koreanSummary,
            emotionSummary = request.emotionSummary,
            keywordsJson = keywordsJson,
        )

        weeklyReportRepository.save(report)

        log.info(
            "[WeeklySafety] 리포트 저장 완료 — memberId={}, weekStart={}, signal={}",
            member.id, weekStart, safetySignal
        )
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────────

    private fun parseSafetySignal(request: WeeklySafetyReportRequest): SafetySignal {
        // 1. 신규 safety_signal 필드 우선 사용
        return try {
            SafetySignal.valueOf(request.safetySignal.uppercase())
        } catch (e: IllegalArgumentException) {
            // 2. 파싱 실패 시 score 기반으로 계산
            if (request.safetyScore > 0) {
                SafetySignal.fromScore(request.safetyScore)
            } else {
                // 3. 하위 호환 — 기존 safety_level 사용
                when (request.safetyLevel) {
                    SafetyLevel.DANGER  -> SafetySignal.RED
                    SafetyLevel.CONCERN -> SafetySignal.YELLOW
                    else                -> SafetySignal.GREEN
                }
            }
        }
    }

    private fun defaultReasonSummary(signal: SafetySignal): String = when (signal) {
        SafetySignal.GREEN  -> "편안하게 대화를 나누고 있어요."
        SafetySignal.YELLOW -> "비슷한 질문을 여러 번 반복하고 있어요."
        SafetySignal.RED    -> "아이의 감정 표현을 조금 더 살펴보는 것이 좋아 보여요."
    }
}
