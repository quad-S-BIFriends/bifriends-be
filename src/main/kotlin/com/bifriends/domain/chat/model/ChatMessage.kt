package com.bifriends.domain.chat.model

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 채팅 메시지 (사용자 발화 + Leo 응답 모두 저장).
 *
 * [memberId]를 비정규화해두는 이유:
 * Leo의 기간별 메시지 조회(GET /api/v1/chat/messages?memberId&from&to) 시
 * chat_sessions JOIN 없이 단일 테이블 범위 스캔으로 처리하기 위함.
 */
@Entity
@Table(
    name = "chat_messages",
    indexes = [
        Index(name = "idx_chat_messages_session", columnList = "session_id"),
        Index(name = "idx_chat_messages_member_created", columnList = "member_id, created_at"),
    ]
)
class ChatMessage(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    val session: ChatSession,

    /** 비정규화 — 기간별 조회 성능 최적화 */
    @Column(name = "member_id", nullable = false, updatable = false)
    val memberId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    val role: MessageRole,

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
