package com.bifriends.domain.chat.repository

import com.bifriends.domain.chat.model.ChatSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChatSessionRepository : JpaRepository<ChatSession, Long> {
    /** FE UUID(sessionKey)로 세션 조회 */
    fun findBySessionKey(sessionKey: String): ChatSession?

    /** 회원의 세션 목록 — 최근 활동 순 */
    @Query("SELECT s FROM ChatSession s WHERE s.member.id = :memberId ORDER BY s.updatedAt DESC")
    fun findByMemberIdOrderByUpdatedAtDesc(@Param("memberId") memberId: Long): List<ChatSession>

    fun deleteBySessionKey(sessionKey: String)
    fun deleteAllByMemberId(memberId: Long)
}
