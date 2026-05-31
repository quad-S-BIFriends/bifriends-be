package com.bifriends.domain.member.repository

import com.bifriends.domain.member.model.Member
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Optional<Member>
    fun findByProviderId(providerId: String): Optional<Member>

    /** 온보딩 완료 회원 전체 — 주간 안전 보고서 배치 대상 */
    fun findAllByOnboardingCompletedTrue(): List<Member>
}
