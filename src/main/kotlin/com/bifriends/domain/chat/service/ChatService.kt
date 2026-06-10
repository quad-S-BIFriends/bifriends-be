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
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
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
     * FE 메시지 전송 — 세션 자동 생성 + 메시지 저장 + AI 중계
     * sessionKey(UUID)로 세션을 조회하고, 없으면 신규 생성한다.
     */
    @Transactional
    fun sendMessage(memberId: Long, request: ChatMessageRequest): ChatMessageResponse {
        // 1. sessionKey로 세션 조회 or 신규 생성
        val session = chatSessionRepository.findBySessionKey(request.sessionId)
            ?: run {
                val member = memberRepository.findById(memberId)
                    .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }
                chatSessionRepository.save(
                    ChatSession(sessionKey = request.sessionId, member = member)
                )
            }

        check(session.member.id == memberId) { "본인의 세션에만 메시지를 보낼 수 있습니다." }

        // 2. 사용자 메시지 저장
        chatMessageRepository.save(
            ChatMessage(
                session = session,
                memberId = memberId,
                role = MessageRole.USER,
                content = request.message,
            )
        )

        // 3. AI 호출
        val aiResponse = aiChatClient.sendChat(
            AiChatRequest(
                memberId = memberId,
                nickname = request.nickname,
                grade = request.grade,
                interests = request.interests,
                sessionId = request.sessionId,
                message = request.message,
            )
        )

        // 4. 어시스턴트 메시지 저장
        aiResponse.reply?.let {
            chatMessageRepository.save(
                ChatMessage(
                    session = session,
                    memberId = memberId,
                    role = MessageRole.ASSISTANT,
                    content = it,
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

    // ── FE API — 세션 목록·상세·삭제 ─────────────────────────────────────────

    /** 내 채팅 세션 목록 — 최근 활동 순 */
    fun getMySessions(memberId: Long): ChatSessionListResponse {
        val sessions = chatSessionRepository.findByMemberIdOrderByUpdatedAtDesc(memberId)
        return ChatSessionListResponse(sessions = sessions.map { ChatSessionSummary.from(it) })
    }

    /** 세션 내 메시지 목록 (FE용) — 본인 세션만 조회 가능 */
    fun getMySessionMessages(memberId: Long, sessionKey: String): ChatSessionMessagesResponse {
        val session = chatSessionRepository.findBySessionKey(sessionKey)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다.")
        if (session.member.id != memberId)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 세션만 조회할 수 있습니다.")
        val messages = chatMessageRepository.findBySessionKeyOrderByCreatedAtAsc(sessionKey)
        return ChatSessionMessagesResponse.from(session, messages)
    }

    /** 세션 삭제 — 메시지 먼저 삭제 후 세션 삭제 */
    @Transactional
    fun deleteSession(memberId: Long, sessionKey: String) {
        val session = chatSessionRepository.findBySessionKey(sessionKey)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "세션을 찾을 수 없습니다.")
        if (session.member.id != memberId)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 세션만 삭제할 수 있습니다.")
        chatMessageRepository.deleteAllBySessionKey(sessionKey)
        chatSessionRepository.deleteBySessionKey(sessionKey)
    }

    // ── Leo 내부 API ──────────────────────────────────────────────────────────

    /** 세션 내 메시지 전체 목록 (Leo 3.5) — sessionKey로 조회 */
    fun getSessionMessages(sessionKey: String): ChatSessionMessagesResponse {
        val session = chatSessionRepository.findBySessionKey(sessionKey)
            ?: throw IllegalArgumentException("세션을 찾을 수 없습니다. sessionKey=$sessionKey")
        val messages = chatMessageRepository.findBySessionKeyOrderByCreatedAtAsc(sessionKey)
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
    fun patchSession(sessionKey: String, request: PatchChatSessionRequest): PatchChatSessionResponse {
        val session = chatSessionRepository.findBySessionKey(sessionKey)
            ?: throw IllegalArgumentException("세션을 찾을 수 없습니다. sessionKey=$sessionKey")
        session.update(title = request.title, status = request.status)
        return PatchChatSessionResponse.from(session)
    }
}
