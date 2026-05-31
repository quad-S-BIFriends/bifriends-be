package com.bifriends.domain.chat.service

import com.bifriends.domain.chat.dto.*
import com.bifriends.domain.chat.model.ChatMessage
import com.bifriends.domain.chat.model.ChatSession
import com.bifriends.domain.chat.model.MessageRole
import com.bifriends.domain.chat.repository.ChatMessageRepository
import com.bifriends.domain.chat.repository.ChatSessionRepository
import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.infrastructure.ai.AiChatClient
import com.bifriends.infrastructure.ai.dto.AiChatRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ChatService(
    private val aiChatClient: AiChatClient,
    private val chatSessionRepository: ChatSessionRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val memberRepository: MemberRepository,
) {

    /**
     * FE에서 받은 메시지를 AI로 중계하고, 사용자·어시스턴트 메시지를 DB에 저장한다.
     *
     * - sessionId가 없으면 신규 세션을 자동 생성한다.
     * - AI 연동이 비활성화(enabled=false)되면 reply=null로 반환하며, 이때도 user 메시지는 저장한다.
     */
    @Transactional
    fun sendMessage(memberId: Long, request: ChatMessageRequest): ChatMessageResponse {
        // 1. 세션 조회 또는 생성
        val session = chatSessionRepository.findById(request.sessionId)
            .orElseGet {
                val member = memberRepository.findById(memberId)
                    .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }
                chatSessionRepository.save(
                    ChatSession(
                        sessionId = request.sessionId,
                        member = member,
                    )
                )
            }

        // 2. 소유권 검증
        check(session.member.id == memberId) { "본인의 세션에만 메시지를 보낼 수 있습니다." }

        // 3. 사용자 메시지 저장
        chatMessageRepository.save(
            ChatMessage(
                session = session,
                memberId = memberId,
                role = MessageRole.USER,
                content = request.message,
            )
        )

        // 4. AI 호출
        val aiRequest = AiChatRequest(
            memberId = memberId,
            nickname = request.nickname,
            grade = request.grade,
            interests = request.interests,
            sessionId = request.sessionId,
            message = request.message,
        )
        val aiResponse = aiChatClient.sendChat(aiRequest)

        // 5. 어시스턴트 메시지 저장 (응답이 있을 때만)
        aiResponse.reply?.let { reply ->
            chatMessageRepository.save(
                ChatMessage(
                    session = session,
                    memberId = memberId,
                    role = MessageRole.ASSISTANT,
                    content = reply,
                )
            )
        }

        return ChatMessageResponse(
            sessionId = request.sessionId,
            reply = aiResponse.reply,
            cta = aiResponse.cta,
            todosCreated = aiResponse.todosCreated,
        )
    }

    // ── Leo 내부 API ──────────────────────────────────────────────────────────

    /** 세션 내 메시지 전체 목록 (Leo 3.5) */
    fun getSessionMessages(sessionId: String): ChatSessionMessagesResponse {
        val session = chatSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("세션을 찾을 수 없습니다. sessionId=$sessionId") }
        val messages = chatMessageRepository.findBySessionSessionIdOrderByCreatedAtAsc(sessionId)
        return ChatSessionMessagesResponse.from(session, messages)
    }

    /** 회원 기간별 메시지 조회 (Leo 3.6) */
    fun getMessagesByRange(
        memberId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): ChatMessagesRangeResponse {
        val messages = chatMessageRepository
            .findByMemberIdAndCreatedAtBetweenWithSession(memberId, from, to)
        return ChatMessagesRangeResponse(
            memberId = memberId,
            from = from,
            to = to,
            messages = messages.map { ChatMessageWithSessionItem.from(it) },
        )
    }

    /** 세션 제목·상태 수정 (Leo 3.9) */
    @Transactional
    fun patchSession(sessionId: String, request: PatchChatSessionRequest): PatchChatSessionResponse {
        val session = chatSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("세션을 찾을 수 없습니다. sessionId=$sessionId") }
        session.update(title = request.title, status = request.status)
        return PatchChatSessionResponse.from(session)
    }
}
