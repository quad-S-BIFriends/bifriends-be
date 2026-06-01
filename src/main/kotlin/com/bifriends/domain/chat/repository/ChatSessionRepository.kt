package com.bifriends.domain.chat.repository

import com.bifriends.domain.chat.model.ChatSession
import org.springframework.data.jpa.repository.JpaRepository

interface ChatSessionRepository : JpaRepository<ChatSession, Long> {
    /** FE UUID(sessionKey)로 세션 조회 */
    fun findBySessionKey(sessionKey: String): ChatSession?
    fun deleteAllByMemberId(memberId: Long)
}
