package com.bifriends.domain.home.model

/**
 * 보상 지급 정책
 *
 * 각 행동에 따라 지급되는 풀(Pool) 양을 정의한다.
 * 보상 규칙이 바뀌면 이 파일만 수정하면 된다.
 */
object RewardPolicy {

    // ── 출석 보상 ──────────────────────────────────────
    // streak 일수에 따라 차등 지급 (매일 00:00 기준 초기화)
    fun attendancePool(streakDays: Int): Int = when {
        streakDays >= 4 -> 5
        streakDays == 3 -> 5
        streakDays == 2 -> 4
        else            -> 3  // 1일 (첫 로그인 / comeback 포함)
    }

    // ── 배움 탭 보상 ───────────────────────────────────
    const val LEARNING_CORRECT = 1           // 문제 1개 정답
    const val LEARNING_SET_COMPLETE = 2      // 3문제 세트 완료 추가 보너스
    //   → 1세트 최대 총 5풀 = (정답 3개 × 1풀) + 세트보너스 2풀

    // ── 마음 탭 보상 ───────────────────────────────────
    const val EMOTION = 3                    // 시나리오 1개 완료

    // ── 할 일 보상 ─────────────────────────────────────
    const val TODO_SINGLE = 1               // 할 일 하나 완료
    const val TODO_ALL_COMPLETE = 3         // 할 일 목록 전체 완료 보너스
}
