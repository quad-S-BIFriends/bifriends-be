package com.bifriends.domain.safety.service

import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.domain.report.model.SafetySignal
import com.bifriends.domain.report.model.WeeklySafetyReport
import com.bifriends.domain.report.repository.WeeklySafetyReportRepository
import com.bifriends.domain.safety.dto.WeeklySafetyReportRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WeeklySafetyService(
    private val weeklySafetyReportRepository: WeeklySafetyReportRepository,
    private val memberRepository: MemberRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun receiveReport(request: WeeklySafetyReportRequest) {
        val member = memberRepository.findById(request.memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=${request.memberId}") }

        // 같은 주차 중복 수신 시 덮어쓰기
        weeklySafetyReportRepository.findByMemberIdAndWeekStart(member.id, request.weekStart)
            ?.let { weeklySafetyReportRepository.delete(it) }

        val safetySignal = try {
            SafetySignal.valueOf(request.safetySignal.uppercase())
        } catch (e: IllegalArgumentException) {
            SafetySignal.fromScore(request.score)
        }

        weeklySafetyReportRepository.save(
            WeeklySafetyReport(
                member = member,
                weekStart = request.weekStart,
                weekEnd = request.weekEnd,
                safetySignal = safetySignal,
                score = request.score,
                reasonSummary = request.reasonSummary,
            )
        )

        log.info(
            "[WeeklySafety] 안전 신호 저장 — memberId={}, weekStart={}, signal={}",
            member.id, request.weekStart, safetySignal
        )
    }
}
