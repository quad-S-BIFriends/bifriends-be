package com.bifriends.domain.report.repository

import com.bifriends.domain.report.model.WeeklyReport
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface WeeklyReportRepository : JpaRepository<WeeklyReport, Long> {
    fun findAllByMemberIdOrderByWeekStartDesc(memberId: Long): List<WeeklyReport>
    fun findByMemberIdAndWeekStart(memberId: Long, weekStart: LocalDate): WeeklyReport?
    fun existsByMemberIdAndWeekStart(memberId: Long, weekStart: LocalDate): Boolean
    fun deleteAllByMemberId(memberId: Long)
}
