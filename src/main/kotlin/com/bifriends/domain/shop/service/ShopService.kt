package com.bifriends.domain.shop.service

import com.bifriends.domain.home.repository.UserStatsRepository
import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.domain.shop.dto.*
import com.bifriends.domain.shop.model.MemberShopItem
import com.bifriends.domain.shop.model.ShopItemCategory
import com.bifriends.domain.shop.repository.MemberShopItemRepository
import com.bifriends.domain.shop.repository.ShopItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ShopService(
    private val shopItemRepository: ShopItemRepository,
    private val memberShopItemRepository: MemberShopItemRepository,
    private val memberRepository: MemberRepository,
    private val userStatsRepository: UserStatsRepository,
) {

    // ── 상점 아이템 목록 (HOM-09-01/03) ──────────────────────────────────────

    fun getShopItems(memberId: Long): ShopItemListResponse {
        val stats = userStatsRepository.findByMemberId(memberId)
            ?: throw IllegalStateException("사용자 통계 정보가 없습니다.")

        val ownedIds = memberShopItemRepository
            .findAllByMemberIdWithItem(memberId)
            .map { it.shopItem.id }
            .toSet()

        val items = shopItemRepository.findAllByIsActiveTrue()
            .map { ShopItemResponse.from(it, owned = it.id in ownedIds) }

        return ShopItemListResponse(
            availablePool = stats.availablePool,
            items = items,
        )
    }

    // ── 나의 서랍 (보유 아이템 + 착용 현황) ─────────────────────────────────

    fun getMyItems(memberId: Long): MyShopItemsResponse {
        val member = findMember(memberId)

        val ownedItems = memberShopItemRepository
            .findAllByMemberIdWithItem(memberId)
            .map { msi ->
                OwnedShopItemResponse(
                    id = msi.shopItem.id,
                    name = msi.shopItem.name,
                    category = msi.shopItem.category,
                    imageKey = msi.shopItem.imageKey,
                    acquiredAt = msi.acquiredAt,
                )
            }

        return MyShopItemsResponse(
            items = ownedItems,
            equipped = member.toEquippedResponse(),
        )
    }

    // ── 아이템 구매 (HOM-09-03/04) ────────────────────────────────────────────

    @Transactional
    fun purchaseItem(memberId: Long, itemId: Long): PurchaseItemResponse {
        val member = findMember(memberId)
        val item = shopItemRepository.findById(itemId)
            .orElseThrow { IllegalArgumentException("아이템을 찾을 수 없습니다. id=$itemId") }

        require(item.isActive) { "현재 판매 중인 아이템이 아닙니다." }

        check(!memberShopItemRepository.existsByMemberIdAndShopItemId(memberId, itemId)) {
            "이미 보유한 아이템입니다."
        }

        val stats = userStatsRepository.findByMemberId(memberId)
            ?: throw IllegalStateException("사용자 통계 정보가 없습니다.")

        // 풀 차감 (잔액 부족 시 UserStats.spendPool에서 예외 발생)
        stats.spendPool(item.price)

        // 소유 이력 저장
        memberShopItemRepository.save(
            MemberShopItem(member = member, shopItem = item)
        )

        return PurchaseItemResponse(
            itemId = item.id,
            itemName = item.name,
            category = item.category,
            imageKey = item.imageKey,
            remainingPool = stats.availablePool,
            acquiredAt = LocalDateTime.now(),
        )
    }

    // ── 아이템 착용 (HOM-09-05) ───────────────────────────────────────────────

    @Transactional
    fun equipItem(memberId: Long, itemId: Long): EquipItemResponse {
        val member = findMember(memberId)
        val item = shopItemRepository.findById(itemId)
            .orElseThrow { IllegalArgumentException("아이템을 찾을 수 없습니다. id=$itemId") }

        check(memberShopItemRepository.existsByMemberIdAndShopItemId(memberId, itemId)) {
            "보유하지 않은 아이템입니다."
        }

        // 카테고리에 맞는 필드에 저장
        when (item.category) {
            ShopItemCategory.HAT        -> member.equippedHatId = itemId
            ShopItemCategory.GLASSES    -> member.equippedGlassesId = itemId
            ShopItemCategory.CLOTHES    -> member.equippedClothesId = itemId
            ShopItemCategory.BACKGROUND -> member.equippedBackgroundId = itemId
        }

        return EquipItemResponse(equipped = member.toEquippedResponse())
    }

    // ── 아이템 해제 ───────────────────────────────────────────────────────────

    @Transactional
    fun unequipItem(memberId: Long, category: ShopItemCategory): EquipItemResponse {
        val member = findMember(memberId)

        when (category) {
            ShopItemCategory.HAT        -> member.equippedHatId = null
            ShopItemCategory.GLASSES    -> member.equippedGlassesId = null
            ShopItemCategory.CLOTHES    -> member.equippedClothesId = null
            ShopItemCategory.BACKGROUND -> member.equippedBackgroundId = null
        }

        return EquipItemResponse(equipped = member.toEquippedResponse())
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────────────

    private fun findMember(memberId: Long) =
        memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

    private fun com.bifriends.domain.member.model.Member.toEquippedResponse() = EquippedItemsResponse(
        hatId = equippedHatId,
        glassesId = equippedGlassesId,
        clothesId = equippedClothesId,
        backgroundId = equippedBackgroundId,
    )
}
