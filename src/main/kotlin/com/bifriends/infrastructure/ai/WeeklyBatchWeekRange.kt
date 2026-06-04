package com.bifriends.infrastructure.ai

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * 주간 배치(안전·성장 리포트)용 [weekStart]~[weekEnd] 계산.
 *
 * 스케줄러는 **월요일 01:00 KST**에 실행되며, 직전 완료 주(월~일)를 집계한다.
 */
object WeeklyBatchWeekRange {
    private val KST = ZoneId.of("Asia/Seoul")

    fun previousCompletedWeek(today: LocalDate = LocalDate.now(KST)): Pair<LocalDate, LocalDate> {
        val weekEnd = today.minusDays(1)
        val weekStart = weekEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return weekStart to weekEnd
    }
}
