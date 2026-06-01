package com.bifriends.domain.report.service

import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.domain.report.dto.WeeklyReportCallbackRequest
import com.bifriends.domain.report.model.WeeklyReport
import com.bifriends.domain.report.repository.WeeklyReportRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WeeklyReportCallbackService(
    private val weeklyReportRepository: WeeklyReportRepository,
    private val memberRepository: MemberRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun receiveReport(request: WeeklyReportCallbackRequest) {
        val member = memberRepository.findById(request.memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=${request.memberId}") }

        // 중복 수신 시 sectionsJson 업데이트
        val existing = weeklyReportRepository.findByMemberIdAndWeekStart(member.id, request.weekStart)
        if (existing != null) {
            existing.updateSections(request.sections)
            log.info("[WeeklyReport] 리포트 업데이트 — memberId={}, weekStart={}", member.id, request.weekStart)
        } else {
            weeklyReportRepository.save(
                WeeklyReport(
                    member = member,
                    weekStart = request.weekStart,
                    weekEnd = request.weekEnd,
                    sectionsJson = request.sections,
                )
            )
            log.info("[WeeklyReport] 리포트 저장 — memberId={}, weekStart={}", member.id, request.weekStart)
        }
    }
}
