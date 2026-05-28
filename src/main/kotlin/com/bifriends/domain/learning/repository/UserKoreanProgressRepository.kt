package com.bifriends.domain.learning.repository

import com.bifriends.domain.learning.model.UserKoreanProgress
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserKoreanProgressRepository : JpaRepository<UserKoreanProgress, Long> {

    fun findByMemberIdAndKoreanStepId(memberId: Long, koreanStepId: Long): UserKoreanProgress?

    @Query("""
        SELECT p FROM UserKoreanProgress p
        JOIN FETCH p.koreanStep
        WHERE p.member.id = :memberId
        AND p.koreanStep.grade = :grade
    """)
    fun findAllByMemberIdAndGrade(memberId: Long, grade: Int): List<UserKoreanProgress>
}
