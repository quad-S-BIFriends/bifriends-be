package com.bifriends.domain.home.dto

import com.bifriends.domain.home.model.UserStats

// ── 조회 응답 ─────────────────────────────────────────────────────────────

data class UserStatsResponse(
    val level: Int,
    val availablePool: Int,
    val totalPoolEarned: Int,
    val streakDays: Int,
    /** 현재 레벨 내 진행 풀 */
    val currentLevelProgress: Int,
    /** 현재 레벨 → 다음 레벨 총 필요 풀 */
    val totalPoolForCurrentLevelUp: Int,
    /** 다음 레벨까지 남은 풀 */
    val poolNeededForNextLevel: Int,
) {
    companion object {
        fun from(stats: UserStats) = UserStatsResponse(
            level = stats.level,
            availablePool = stats.availablePool,
            totalPoolEarned = stats.totalPoolEarned,
            streakDays = stats.streakDays,
            currentLevelProgress = stats.currentLevelProgress(),
            totalPoolForCurrentLevelUp = stats.totalPoolForCurrentLevelUp(),
            poolNeededForNextLevel = stats.poolNeededForNextLevel(),
        )
    }
}

// ── 보상 결과 ─────────────────────────────────────────────────────────────

/** 풀 획득 후 반환하는 결과 */
data class RewardResult(
    /** 이번에 획득한 풀 */
    val earnedPool: Int,
    /** 보상 지급 후 현재 사용 가능한 풀 */
    val availablePool: Int,
    /** 보상 지급 후 누적 획득 풀 */
    val totalPoolEarned: Int,
    /** 보상 지급 전 레벨 */
    val levelBefore: Int,
    /** 보상 지급 후 레벨 */
    val levelAfter: Int,
) {
    val leveledUp: Boolean get() = levelAfter > levelBefore
}

// ── 출석 결과 ─────────────────────────────────────────────────────────────

/** 출석 처리 후 반환하는 결과 */
data class AttendanceResult(
    /** 오늘 출석이 처음인 경우 true (보상 지급됨) */
    val isFirstAttendanceToday: Boolean,
    /** 연속 출석 일수 */
    val streakDays: Int,
    /** 지급된 보상 (출석이 이미 처리된 경우 null) */
    val reward: RewardResult?,
)
