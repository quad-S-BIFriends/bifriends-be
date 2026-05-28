package com.bifriends.domain.member.dto

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

data class RepresentativeItemResponse(
    val representativeItemType: ItemType
)