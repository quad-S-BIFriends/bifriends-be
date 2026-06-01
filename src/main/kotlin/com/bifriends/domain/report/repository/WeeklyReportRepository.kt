package com.bifriends.domain.report.repository

import com.bifriends.domain.report.model.WeeklyReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface WeeklyReportRepository : JpaRepository<WeeklyReport, Long> {

    /** 회원의 리포트 목록 (최신순) — RPT-02 */
    fun findAllByMemberIdOrderByWeekStartDesc(memberId: Long): List<WeeklyReport>

    /** 특정 주차 리포트 조회 — RPT-03 */
    fun findByMemberIdAndWeekStart(memberId: Long, weekStart: LocalDate): WeeklyReport?

    /** 해당 주차 리포트 존재 여부 (중복 저장 방지) */
    fun existsByMemberIdAndWeekStart(memberId: Long, weekStart: LocalDate): Boolean
    fun deleteAllByMemberId(memberId: Long)
}
