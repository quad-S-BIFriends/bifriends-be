package com.bifriends.domain.member.model
import com.bifriends.domain.onboarding.model.ItemType
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "members")
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val name: String,

    @Column
    val profileImageUrl: String? = null,

    @Column(nullable = false, unique = true)
    val providerId: String,

    @Column(nullable = false)
    val provider: String = "google",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role = Role.ROLE_USER,

    @Column
    var nickname: String? = null,

    @Column
    var grade: Int? = null,

    @Column
    var guardianPhone: String? = null,

    @Column(nullable = false)
    var notificationEnabled: Boolean = false,

    @Column(nullable = false)
    var microphoneEnabled: Boolean = false,

    @Column(nullable = false)
    var onboardingCompleted: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column
    var representativeItemType: ItemType? = null,


    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var lastLoginAt: LocalDateTime = LocalDateTime.now()
) {
    fun updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now()
    }
}
