package com.bifriends.domain.onboarding.repository

import com.bifriends.domain.onboarding.model.MemberItem
import org.springframework.data.jpa.repository.JpaRepository

interface MemberItemRepository : JpaRepository<MemberItem, Long> {
    fun findAllByMemberId(memberId: Long): List<MemberItem>
    fun deleteAllByMemberId(memberId: Long)
}
