package com.bifriends.domain.chat.model

import com.bifriends.domain.member.model.Member
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 레오와의 대화 세션.
 *
 * - sessionId: FE가 UUID를 생성해 첫 메시지와 함께 전달한다.
 *   BE는 해당 ID가 없으면 자동 생성한다 (FE 별도 세션 생성 API 불필요).
 * - title: Leo가 대화 흐름을 파악한 뒤 PATCH로 설정한다.
 * - status: Leo가 세션을 닫을 때 CLOSED로 변경한다.
 */
@Entity
@Table(
    name = "chat_sessions",
    indexes = [
        Index(name = "idx_chat_sessions_member", columnList = "member_id"),
    ]
)
class ChatSession(

    @Id
    @Column(name = "session_id", nullable = false, updatable = false, length = 100)
    val sessionId: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @Column
    var title: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: SessionStatus = SessionStatus.ACTIVE,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {

    /** Leo가 대화 내용을 파악한 뒤 세션 제목과 상태를 수정한다. */
    fun update(title: String?, status: SessionStatus?) {
        title?.let { this.title = it }
        status?.let { this.status = it }
        this.updatedAt = LocalDateTime.now()
    }
}
