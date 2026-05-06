package com.bifriends.domain.home.model

/**
 * 레벨 시스템 정책
 *
 * 레벨업 요구 풀은 선형 증가: Lv N → N+1 에 필요한 풀 = 10 + (N-1) * 5
 *   Lv1→2: 10풀, Lv2→3: 15풀, Lv3→4: 20풀, ...
 *
 * 레벨 N에 도달하기 위한 누적 풀 = 5 * (N-1) * (N+2) / 2
 *   Lv2: 10, Lv3: 25, Lv4: 45, Lv5: 70, ...
 *
 * 핵심 설계 원칙:
 * - totalPoolEarned(누적 획득 풀)는 절대 감소하지 않는다 → 레벨은 내려가지 않는다(레벨은 학습 진행 기록임으로 이에 대한건 건드리면 안됨)
 * - availablePool(사용 가능 풀)은 상점 소비 시 감소할 수 있다
 */
object LevelPolicy {

    /**
     * Lv [fromLevel] → [fromLevel]+1 에 필요한 풀
     */
    fun poolRequiredForLevelUp(fromLevel: Int): Int {
        require(fromLevel >= 1) { "레벨은 1 이상이어야 합니다." }
        return 10 + (fromLevel - 1) * 5
    }

    /**
     * 레벨 [level]에 도달하기 위해 필요한 누적 풀 (레벨 1 시작 기준)
     * 공식: 5 * (N-1) * (N+2) / 2  (단, N = level)
     */
    fun cumulativePoolForLevel(level: Int): Int {
        require(level >= 1) { "레벨은 1 이상이어야 합니다." }
        if (level == 1) return 0
        return 5 * (level - 1) * (level + 2) / 2
    }

    /**
     * 누적 획득 풀로부터 현재 레벨 계산
     */
    fun calculateLevel(totalPoolEarned: Int): Int {
        require(totalPoolEarned >= 0) { "누적 풀은 0 이상이어야 합니다." }
        var level = 1
        while (cumulativePoolForLevel(level + 1) <= totalPoolEarned) {
            level++
        }
        return level
    }

    /**
     * 현재 레벨 내에서의 진행도 (현재 레벨 진입 후 획득한 풀)
     */
    fun progressInCurrentLevel(totalPoolEarned: Int): Int {
        val currentLevel = calculateLevel(totalPoolEarned)
        return totalPoolEarned - cumulativePoolForLevel(currentLevel)
    }

    /**
     * 다음 레벨까지 남은 풀
     */
    fun poolNeededForNextLevel(totalPoolEarned: Int): Int {
        val currentLevel = calculateLevel(totalPoolEarned)
        val progress = progressInCurrentLevel(totalPoolEarned)
        return poolRequiredForLevelUp(currentLevel) - progress
    }
}
