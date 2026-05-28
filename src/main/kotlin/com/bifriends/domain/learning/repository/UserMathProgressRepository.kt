package com.bifriends.domain.learning.repository

import com.bifriends.domain.learning.model.UserMathProgress
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserMathProgressRepository : JpaRepository<UserMathProgress, Long> {

    fun findByMemberIdAndMathStepId(memberId: Long, mathStepId: Long): UserMathProgress?

    @Query("""
        SELECT p FROM UserMathProgress p
        JOIN FETCH p.mathStep
        WHERE p.member.id = :memberId
        AND p.mathStep.grade = :grade
    """)
    fun findAllByMemberIdAndGrade(memberId: Long, grade: Int): List<UserMathProgress>

    fun existsByMemberIdAndMathStepId(memberId: Long, mathStepId: Long): Boolean
}
