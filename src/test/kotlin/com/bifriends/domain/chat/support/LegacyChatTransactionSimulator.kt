package com.bifriends.domain.chat.support

import com.bifriends.domain.chat.model.ChatMessage
import com.bifriends.domain.chat.model.ChatSession
import com.bifriends.domain.chat.model.MessageRole
import com.bifriends.domain.chat.repository.ChatMessageRepository
import com.bifriends.domain.chat.repository.ChatSessionRepository
import com.bifriends.domain.member.repository.MemberRepository
import org.springframework.transaction.annotation.Transactional

/**
 * 수정 전 패턴 재현: user 저장 → (트랜잭션 유지) AI 대기 → assistant 저장.
 * 부하 테스트에서 Hikari 커넥션 점유 시간을 비교하기 위한 테스트 전용 코드.
 */
class LegacyChatTransactionSimulator(
    private val chatSessionRepository: ChatSessionRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val memberRepository: MemberRepository,
) {

    @Transactional
    fun sendMessageHoldingConnection(
        memberId: Long,
        sessionKey: String,
        message: String,
        aiDelayMs: Long,
        aiReply: String,
    ) {
        val session = resolveSession(memberId, sessionKey)
        chatMessageRepository.save(
            ChatMessage(
                session = session,
                memberId = memberId,
                role = MessageRole.USER,
                content = message,
            ),
        )
        Thread.sleep(aiDelayMs)
        chatMessageRepository.save(
            ChatMessage(
                session = session,
                memberId = memberId,
                role = MessageRole.ASSISTANT,
                content = aiReply,
            ),
        )
    }

    private fun resolveSession(memberId: Long, sessionKey: String): ChatSession {
        val session = chatSessionRepository.findBySessionKey(sessionKey)
            ?: run {
                val member = memberRepository.findById(memberId)
                    .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }
                chatSessionRepository.save(ChatSession(sessionKey = sessionKey, member = member))
            }
        check(session.member.id == memberId) { "본인의 세션에만 메시지를 보낼 수 있습니다." }
        return session
    }
}
