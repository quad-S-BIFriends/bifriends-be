package com.bifriends.domain.home.repository

import com.bifriends.domain.home.model.RewardHistory
import com.bifriends.domain.home.model.RewardSource
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface RewardHistoryRepository : JpaRepository<RewardHistory, Long> {

    fun findAllByMemberIdOrderByCreatedAtDesc(memberId: Long): List<RewardHistory>

    /**
     * 특정 날짜 범위 내 출석 보상 지급 여부 확인
     * (멱등성 체크 보조 용도)
     */
    fun existsByMemberIdAndSourceAndCreatedAtBetween(
        memberId: Long,
        source: RewardSource,
        from: LocalDateTime,
        to: LocalDateTime,
    ): Boolean
}
