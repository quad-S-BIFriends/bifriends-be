package com.bifriends.domain.learning.repository

import com.bifriends.domain.learning.model.LearningAttempt
import com.bifriends.domain.learning.model.LearningSubject
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface LearningAttemptRepository : JpaRepository<LearningAttempt, Long> {

    /** 특정 문제의 현재 진행 중인 시도 조회 (upsert 패턴용) */
    fun findByMemberIdAndStepIdAndCycleNumberAndQuestionIndex(
        memberId: Long,
        stepId: Long,
        cycleNumber: Int,
        questionIndex: Int,
    ): LearningAttempt?

    /**
     * 주간 개념별 집계 (학습 요약 API용)
     * solved = true 인 것만 집계, solved = 0인 개념은 제외
     */
    @Query("""
        SELECT la.concept,
               COUNT(la) AS solved,
               AVG(la.attempts) AS avgAttempts,
               AVG(la.hintsUsed) AS avgHints
        FROM LearningAttempt la
        WHERE la.member.id = :memberId
          AND la.subject = :subject
          AND la.solved = true
          AND la.solvedAt BETWEEN :from AND :to
        GROUP BY la.concept
        ORDER BY la.concept
    """)
    fun findWeeklySummaryBySubject(
        @Param("memberId") memberId: Long,
        @Param("subject") subject: LearningSubject,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): List<Array<Any>>

    fun deleteAllByMemberId(memberId: Long)
}
