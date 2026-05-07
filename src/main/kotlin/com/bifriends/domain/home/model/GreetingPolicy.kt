package com.bifriends.domain.home.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 인사 메시지 정책
 *
 * 홈 화면 진입 시 노출할 인사 메시지 유형 결정 + 메시지 풀 관리.
 *
 * ─────────────────────────────────────────────────────────────────
 * 분기 우선순위 (Notion 명세 기준)
 * ─────────────────────────────────────────────────────────────────
 * 1. first_login  : lastAttendanceDate == null (최초 접속)
 * 2. comeback     : gap ≥ 2 (2일 이상 공백)
 *    - comeback_short : gap 2~3일
 *    - comeback_long  : gap 4일 이상
 * 3. streak       : gap ≤ 1 (연속 출석)
 *    - streak_day_1   : streakDays == 1
 *    - streak_day_2_3 : streakDays 2~3
 *    - streak_day_4_6 : streakDays 4~6
 *    - streak_day_7+  : streakDays 7 이상
 * ─────────────────────────────────────────────────────────────────
 *
 * LevelPolicy 와 동일하게 순수 object 로 분리해
 * Spring/DB 없이 단위 테스트에서 바로 호출 가능하다.
 */
object GreetingPolicy {

    // ── 메시지 풀 (Notion 명세 message_groups) ────────────────────────────

    private val FIRST_LOGIN_MESSAGES = listOf(
        "안녕, {name}! 만나서 반가워 🙌",
        "반가워, {name}! 같이 차근차근 해보자 💪",
    )

    private val COMEBACK_SHORT_MESSAGES = listOf(
        "와 {name}! 또 만나서 좋다 😆",
        "다시 만나서 너무 좋아 ☺️, {name}!",
    )

    private val COMEBACK_LONG_MESSAGES = listOf(
        "우와 {name}! 오랜만이다 😆",
        "{name} 다시 만나서 진짜 반가워 😊",
        "어! {name} 왔다 🎉 기다렸어",
    )

    private val STREAK_DAY_1_MESSAGES = listOf(
        "안녕, {name}! 오늘도 반가워 💕",
        "안녕 {name}! 오늘도 파이팅!📚",
    )

    private val STREAK_DAY_2_3_MESSAGES = listOf(
        "오 {name}! 벌써 또 왔네 😆",
        "{name} 계속 하고 있네! 멋지다 🎉",
        "와 {name}! 잘하고 있어 😊",
    )

    private val STREAK_DAY_4_6_MESSAGES = listOf(
        "우와 {name}! 진짜 잘하고 있다 😆",
        "{name} 완전 꾸준한데? 멋져 🎉",
        "대박 {name}! 계속 이어가고 있네 😊",
    )

    private val STREAK_DAY_7_PLUS_MESSAGES = listOf(
        "와 {name}! 진짜 꾸준하다 😆",
        "{name} 완전 멋진데? 🎉",
        "대단해 {name}! 계속 잘하고 있어 😊",
    )

    // ── 유형 결정 ────────────────────────────────────────────────────────

    /**
     * 출석 처리 전(前) lastAttendanceDate 를 기준으로 인사 유형을 결정한다.
     *
     * gap = ChronoUnit.DAYS.between(lastAttendanceDate, today)
     * - null  → FIRST_LOGIN
     * - 0~1   → STREAK  (오늘 재방문 or 어제 연속 출석)
     * - 2~3   → COMEBACK_SHORT
     * - 4+    → COMEBACK_LONG
     *
     * @param lastAttendanceDateBefore 출석 처리 전 lastAttendanceDate (최초 접속이면 null)
     * @param today KST 기준 오늘 날짜
     */
    fun determineType(lastAttendanceDateBefore: LocalDate?, today: LocalDate): GreetingType {
        if (lastAttendanceDateBefore == null) return GreetingType.FIRST_LOGIN

        val gapDays = ChronoUnit.DAYS.between(lastAttendanceDateBefore, today)

        return when {
            gapDays <= 1L -> GreetingType.STREAK
            gapDays <= 3L -> GreetingType.COMEBACK_SHORT
            else          -> GreetingType.COMEBACK_LONG
        }
    }

    // ── 메시지 선택 ──────────────────────────────────────────────────────

    /**
     * 인사 유형과 출석 후 streak 일수로 메시지를 무작위 선택하고 닉네임을 치환한다.
     *
     * STREAK 세부 bucket:
     * - streakDays == 1   → streak_day_1
     * - streakDays 2~3    → streak_day_2_3
     * - streakDays 4~6    → streak_day_4_6
     * - streakDays 7+     → streak_day_7_plus
     *
     * @param type      결정된 인사 유형
     * @param streakDays 출석 처리 후 현재 streak 일수
     * @param nickname  {name} 에 치환할 닉네임
     */
    fun selectMessage(type: GreetingType, streakDays: Int, nickname: String): String {
        val pool = when (type) {
            GreetingType.FIRST_LOGIN    -> FIRST_LOGIN_MESSAGES
            GreetingType.COMEBACK_SHORT -> COMEBACK_SHORT_MESSAGES
            GreetingType.COMEBACK_LONG  -> COMEBACK_LONG_MESSAGES
            GreetingType.STREAK         -> when (streakDays) {
                1        -> STREAK_DAY_1_MESSAGES
                in 2..3  -> STREAK_DAY_2_3_MESSAGES
                in 4..6  -> STREAK_DAY_4_6_MESSAGES
                else     -> STREAK_DAY_7_PLUS_MESSAGES
            }
        }
        return pool.random().replace("{name}", nickname)
    }
}
