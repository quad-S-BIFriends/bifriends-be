package com.bifriends.domain.home.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 요일별 학습 과목 분기 정책
 *
 * 월·수·금 → 수학 (MATH)
 * 화·목·토 → 국어 (LANGUAGE)
 * 일        → null (전체 공부방 목록으로 이동, 자유 선택)
 */

 //판단,연산을 하는 코드

 
object LearningTypePolicy {

    fun forDate(date: LocalDate): LearningType? = when (date.dayOfWeek) {
        DayOfWeek.MONDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.FRIDAY    -> LearningType.MATH

        DayOfWeek.TUESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.SATURDAY  -> LearningType.LANGUAGE

        DayOfWeek.SUNDAY    -> null
        else                -> null
    }
}
