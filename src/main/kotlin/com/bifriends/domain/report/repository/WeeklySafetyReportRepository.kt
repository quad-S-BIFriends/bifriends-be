package com.bifriends.domain.report.repository

import com.bifriends.domain.report.model.WeeklySafetyReport
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface WeeklySafetyReportRepository : JpaRepository<WeeklySafetyReport, Long> {
    fun findAllByMemberIdOrderByWeekStartDesc(memberId: Long): List<WeeklySafetyReport>
    fun findByMemberIdAndWeekStart(memberId: Long, weekStart: LocalDate): WeeklySafetyReport?
    fun existsByMemberIdAndWeekStart(memberId: Long, weekStart: LocalDate): Boolean
    fun deleteAllByMemberId(memberId: Long)
}
