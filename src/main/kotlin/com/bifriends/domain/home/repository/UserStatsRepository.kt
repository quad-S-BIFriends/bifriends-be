package com.bifriends.domain.home.repository

import com.bifriends.domain.home.model.UserStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserStatsRepository : JpaRepository<UserStats, Long> {

    fun findByMemberId(memberId: Long): UserStats?

    /**
     * 락을 걸어 출석 처리 등 동시 요청 시 중복 보상을 방지한다.
     * 홈 진입 시 streak 업데이트에 사용.
     */
    @Query("SELECT u FROM UserStats u WHERE u.member.id = :memberId")
    fun findByMemberIdWithLock(memberId: Long): UserStats?
}
