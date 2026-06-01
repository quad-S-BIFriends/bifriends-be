package com.bifriends.domain.chat.model

import com.bifriends.domain.member.model.Member
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 레오와의 대화 세션.
 *
 * - id: DB auto-increment PK (bigint)
 * - sessionKey: FE가 생성한 UUID. 외부 식별자로 사용. (unique)
 * - title: Leo가 첫 메시지 보고 자동 생성
 */
@Entity
@Table(
    name = "chat_sessions",
    indexes = [
        Index(name = "idx_chat_sessions_member", columnList = "member_id"),
        Index(name = "idx_chat_sessions_key", columnList = "session_key"),
    ]
)
class ChatSession(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** FE가 생성한 UUID — 외부 식별자, API 요청/응답에 사용 */
    @Column(name = "session_key", nullable = false, unique = true, updatable = false, length = 100)
    val sessionKey: String,

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
    fun update(title: String?, status: SessionStatus?) {
        title?.let { this.title = it }
        status?.let { this.status = it }
        this.updatedAt = LocalDateTime.now()
    }
}
