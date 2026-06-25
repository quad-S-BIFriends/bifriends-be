package com.bifriends.domain.chat.service

import com.bifriends.domain.chat.model.ChatMessage
import com.bifriends.domain.chat.model.ChatSession
import com.bifriends.domain.chat.model.MessageRole
import com.bifriends.domain.chat.repository.ChatMessageRepository
import com.bifriends.domain.chat.repository.ChatSessionRepository
import com.bifriends.domain.member.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 채팅 메시지 DB 저장 전용.
 * 외부 AI 호출은 트랜잭션 밖에서 수행하기 위해 [ChatService]와 분리한다.
 */
@Service
class ChatMessageWriteService(
    private val chatSessionRepository: ChatSessionRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val memberRepository: MemberRepository,
) {

    @Transactional
    fun saveUserMessage(memberId: Long, sessionKey: String, content: String) {
        val session = resolveSession(memberId, sessionKey)
        chatMessageRepository.save(
            ChatMessage(
                session = session,
                memberId = memberId,
                role = MessageRole.USER,
                content = content,
            )
        )
    }

    @Transactional
    fun saveAssistantMessage(memberId: Long, sessionKey: String, content: String) {
        val session = chatSessionRepository.findBySessionKey(sessionKey)!!
        chatMessageRepository.save(
            ChatMessage(
                session = session,
                memberId = memberId,
                role = MessageRole.ASSISTANT,
                content = content,
            )
        )
    }

    private fun resolveSession(memberId: Long, sessionKey: String): ChatSession {
        val session = chatSessionRepository.findBySessionKey(sessionKey)
            ?: run {
                val member = memberRepository.findById(memberId)
                    .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }
                chatSessionRepository.save(
                    ChatSession(sessionKey = sessionKey, member = member)
                )
            }

        check(session.member.id == memberId) { "본인의 세션에만 메시지를 보낼 수 있습니다." }
        return session
    }
}
