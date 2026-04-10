package com.bifriends.domain.member.repository

import com.bifriends.domain.member.model.Member
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByEmail(email: String): Optional<Member>
    fun findByProviderId(providerId: String): Optional<Member>
}
