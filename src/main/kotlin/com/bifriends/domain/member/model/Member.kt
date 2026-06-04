package com.bifriends.domain.member.model
import com.bifriends.domain.onboarding.model.ItemType
import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
@Table(name = "members")
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val email: String,

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

    // ── 약관 동의 (ONB-02) ──────────────────────────────────────────
    /** 서비스 이용약관 동의 (필수) */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    var termsAgreed: Boolean = false,

    /** 개인정보 처리방침 동의 (필수) */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    var privacyAgreed: Boolean = false,

    /** 마케팅 수신 동의 (선택) */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    var marketingAgreed: Boolean = false,

    /** 약관 동의 시각 — 법적 근거 보관용 (KST) */
    @Column
    var termsAgreedAt: LocalDateTime? = null,

    // ── 부모 모드 (ONB-02-01 / RPT-01) ─────────────────────────────
    /**
     * 부모 모드 PIN (BCrypt 해시).
     * 온보딩 시 설정하며, 이후 부모 모드 진입 및 변경 시 사용한다.
     */
    @Column
    var parentPassword: String? = null,

    /** 착용 중인 전체 의상 코드 (예: GIFT_3, OUTFIT_DEFAULT) */
    @Column(name = "equipped_outfit_code", length = 32)
    var equippedOutfitCode: String? = null,

    // ── 레거시 카테고리별 착용 (구 시드; 신규는 equippedOutfitCode만 사용) ──
    @Column(name = "equipped_hat_id")
    var equippedHatId: Long? = null,

    @Column(name = "equipped_glasses_id")
    var equippedGlassesId: Long? = null,

    @Column(name = "equipped_clothes_id")
    var equippedClothesId: Long? = null,

    @Column(name = "equipped_background_id")
    var equippedBackgroundId: Long? = null,

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
