package com.bifriends.domain.chat.repository

import com.bifriends.domain.chat.model.ChatMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {

    /** 세션 내 메시지 목록 (sessionKey 기반) */
    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.session.sessionKey = :sessionKey
        ORDER BY m.createdAt ASC
    """)
    fun findBySessionKeyOrderByCreatedAtAsc(@Param("sessionKey") sessionKey: String): List<ChatMessage>

    /** 회원 기간별 메시지 조회 — session fetch join으로 N+1 방지 */
    @Query("""
        SELECT m FROM ChatMessage m
        JOIN FETCH m.session
        WHERE m.memberId = :memberId
          AND m.createdAt BETWEEN :from AND :to
        ORDER BY m.createdAt ASC
    """)
    fun findByMemberIdAndCreatedAtBetweenWithSession(
        @Param("memberId") memberId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): List<ChatMessage>

    fun deleteAllByMemberId(memberId: Long)
}
