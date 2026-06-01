package com.bifriends.domain.member.dto

import com.bifriends.domain.learning.dto.KoreanStepsResponse
import com.bifriends.domain.learning.dto.MathStepsResponse
import com.bifriends.domain.onboarding.model.Interest
import com.bifriends.domain.onboarding.model.ItemType
import jakarta.validation.constraints.NotNull

/** AI 내부 호출용 — nickname / grade / interests 만 반환 */
data class MemberProfileSummaryResponse(
    val memberId: Long,
    val nickname: String?,
    val grade: Int?,
    val interests: List<Interest>,
)

data class MemberProfileResponse(
    val id: Long,
    val email: String,
    val nickname: String?,
    val profileImageUrl: String?,
    val grade: Int?,
    val interests: List<Interest>,
    val items: List<MemberItemInfo>,
    val representativeItemType: ItemType?,
    val onboardingCompleted: Boolean
)

data class MemberItemInfo(
    val itemType: ItemType,
    val acquiredAt: String
)

data class RepresentativeItemRequest(
    @field:NotNull(message = "아이템을 선택해야 합니다.")
    val itemType: ItemType
)

// ── HOM-10. 설정 화면 — 이름·학년·관심사 한 번에 저장 ──────────────────────────

data class MemberSettingsRequest(
    /** 이름 (HOM-10-01) — null이면 변경 안 함 */
    @field:jakarta.validation.constraints.Size(min = 1, max = 20, message = "이름은 1~20자여야 합니다.")
    val nickname: String? = null,

    /** 학년 (HOM-10-02) — null이면 변경 안 함 */
    @field:jakarta.validation.constraints.Min(3)
    @field:jakarta.validation.constraints.Max(6)
    val grade: Int? = null,

    /** 관심사 (HOM-10-03) — null이면 변경 안 함, 빈 리스트면 전체 삭제 */
    @field:jakarta.validation.constraints.Size(max = 3, message = "관심사는 최대 3개까지 선택할 수 있습니다.")
    val interests: List<Interest>? = null,
)

data class MemberSettingsResponse(
    val nickname: String?,
    val grade: Int?,
    val interests: List<Interest>,
)

data class RepresentativeItemResponse(
    val representativeItemType: ItemType
)

// ───────────────────────────────────────────────────────────────
// Leo 연동 — 통합 학습 진도 (InternalServicePaths)
// ───────────────────────────────────────────────────────────────

data class LearningProgressResponse(
    val memberId: Long,
    val math: MathStepsResponse,
    val korean: KoreanStepsResponse,
)