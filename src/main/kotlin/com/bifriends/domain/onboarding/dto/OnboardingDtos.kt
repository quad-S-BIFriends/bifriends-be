package com.bifriends.domain.onboarding.dto

import com.bifriends.domain.onboarding.model.Interest
import com.bifriends.domain.onboarding.model.ItemType
import jakarta.validation.constraints.*

// ── ONB-02. 약관 동의 ──────────────────────────────────────────────────────────

data class TermsConsentRequest(
    /** 서비스 이용약관 동의 (필수) */
    @field:AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
    val termsAgreed: Boolean,

    /** 개인정보 처리방침 동의 (필수) */
    @field:AssertTrue(message = "개인정보 처리방침에 동의해야 합니다.")
    val privacyAgreed: Boolean,

    /** 마케팅 수신 동의 (선택) */
    val marketingAgreed: Boolean = false,
)

data class TermsConsentResponse(
    val termsAgreed: Boolean,
    val privacyAgreed: Boolean,
    val marketingAgreed: Boolean,
    val agreedAt: String,
)

// ── ONB-02-01. 부모 비밀번호 설정 ───────────────────────────────────────────────

data class SetParentPasswordRequest(
    /** 4~6자리 숫자 PIN */
    @field:NotBlank(message = "비밀번호를 입력해주세요.")
    @field:Pattern(regexp = "\\d{4,6}", message = "비밀번호는 4~6자리 숫자여야 합니다.")
    val password: String,

    @field:NotBlank(message = "비밀번호 확인을 입력해주세요.")
    val passwordConfirm: String,
)

data class SetParentPasswordResponse(
    val configured: Boolean,
)

// ── ONB-04/06. 프로필 (이름·학년) ──────────────────────────────────────────────

data class ProfileRequest(
    @field:Size(min = 1, max = 20, message = "이름은 1~20자여야 합니다.")
    val nickname: String? = null,

    @field:Min(value = 3, message = "학년은 3 이상이어야 합니다.")
    @field:Max(value = 6, message = "학년은 6 이하여야 합니다.")
    val grade: Int? = null,
)

data class ProfileResponse(
    val nickname: String?,
    val grade: Int?,
)

// ── ONB-07. 관심사 ─────────────────────────────────────────────────────────────

data class InterestsRequest(
    @field:NotEmpty(message = "관심사를 최소 1개 선택해야 합니다.")
    @field:Size(max = 3, message = "관심사는 최대 3개까지 선택할 수 있습니다.")
    val interests: List<Interest>,
)

data class InterestsResponse(
    val interests: List<Interest>,
)

// ── ONB-08. 선물 아이템 ────────────────────────────────────────────────────────

data class GiftRequest(
    @field:NotNull(message = "아이템을 선택해야 합니다.")
    val itemType: ItemType,
)

data class GiftResponse(
    val itemType: ItemType,
    val acquiredAt: String,
)

// ── ONB-10. 권한 설정 ──────────────────────────────────────────────────────────

data class PermissionsRequest(
    val notificationEnabled: Boolean,
    val microphoneEnabled: Boolean,
)

data class PermissionsResponse(
    val notificationEnabled: Boolean,
    val microphoneEnabled: Boolean,
)

// ── ONB-11. 온보딩 완료 ────────────────────────────────────────────────────────

data class OnboardingCompleteResponse(
    val completed: Boolean,
)
