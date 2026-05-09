package com.bifriends.domain.member.dto

import com.bifriends.domain.onboarding.model.Interest
import com.bifriends.domain.onboarding.model.ItemType
import jakarta.validation.constraints.NotNull

data class MemberProfileResponse(
    val id: Long,
    val email: String,
    val name: String,
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