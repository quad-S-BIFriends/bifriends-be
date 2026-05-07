package com.bifriends.domain.home.model

/**
 * 인사 메시지 유형
 *
 * 홈 화면 진입 시 노출되는 인사 메시지의 분기 기준.
 * 출석 처리 전(前) lastAttendanceDate 와 오늘 날짜 간의 gap으로 결정된다.
 *
 * ```
 * lastAttendanceDate == null  → FIRST_LOGIN
 * gap ≤ 1일              → STREAK      (오늘 재방문 or 어제 연속 출석)
 * gap 2~3일              → COMEBACK_SHORT
 * gap 4일 이상           → COMEBACK_LONG
 * ```
 *
 * STREAK 세부 bucket (streak_day_1 / 2~3 / 4~6 / 7+)은
 * GreetingPolicy.selectMessage() 에서 streakDays 값으로 결정된다.
 */
enum class GreetingType {
    /** 앱 최초 접속 */
    FIRST_LOGIN,

    /** 2~3일 만에 복귀 */
    COMEBACK_SHORT,

    /** 4일 이상 만에 복귀 */
    COMEBACK_LONG,

    /** 연속 출석 중 (gap ≤ 1일) */
    STREAK,
}
