package com.bifriends.domain.shop.service

import com.bifriends.domain.home.repository.UserStatsRepository
import com.bifriends.domain.member.repository.MemberRepository
import com.bifriends.domain.onboarding.repository.MemberItemRepository
import com.bifriends.domain.shop.dto.*
import com.bifriends.domain.shop.model.MemberShopItem
import com.bifriends.domain.shop.model.ShopItem
import com.bifriends.domain.shop.model.ShopItemCategory
import com.bifriends.domain.shop.model.ShopOutfitCodes
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
    private val memberItemRepository: MemberItemRepository,
    private val memberRepository: MemberRepository,
    private val userStatsRepository: UserStatsRepository,
) {

    fun getShopItems(memberId: Long): ShopItemListResponse {
        val stats = userStatsRepository.findByMemberId(memberId)
            ?: throw IllegalStateException("사용자 통계 정보가 없습니다.")

        val ownedCodes = resolveOwnedOutfitCodes(memberId)

        val items = shopItemRepository.findAllByIsActiveTrueOrderByPriceAscIdAsc()
            .filter { it.category == ShopItemCategory.OUTFIT }
            .map { ShopItemResponse.from(it, owned = it.itemCode in ownedCodes) }

        return ShopItemListResponse(
            availablePool = stats.availablePool,
            items = items,
        )
    }

    fun getMyItems(memberId: Long): MyShopItemsResponse {
        val member = findMember(memberId)
        val ownedCodes = resolveOwnedOutfitCodes(memberId)
        val catalogByCode = shopItemRepository.findAllByIsActiveTrueOrderByPriceAscIdAsc()
            .filter { it.category == ShopItemCategory.OUTFIT }
            .associateBy { it.itemCode }

        val purchasedAtByCode = memberShopItemRepository
            .findAllByMemberIdWithItem(memberId)
            .associate { it.shopItem.itemCode to it.acquiredAt }

        val items = ownedCodes.mapNotNull { code ->
            val item = catalogByCode[code] ?: return@mapNotNull null
            OwnedShopItemResponse(
                itemCode = item.itemCode,
                name = item.name,
                category = item.category,
                imageKey = item.imageKey,
                acquiredAt = purchasedAtByCode[code],
            )
        }

        return MyShopItemsResponse(
            items = items,
            equipped = member.toEquippedResponse(),
        )
    }

    @Transactional
    fun purchaseItem(memberId: Long, itemCode: String): PurchaseItemResponse {
        val member = findMember(memberId)
        val item = findActiveOutfit(itemCode)

        check(!isOwned(memberId, item)) {
            "이미 보유한 아이템입니다."
        }

        require(item.price > 0) { "무료 의상은 구매할 수 없습니다." }

        val stats = userStatsRepository.findByMemberId(memberId)
            ?: throw IllegalStateException("사용자 통계 정보가 없습니다.")

        stats.spendPool(item.price)

        val acquiredAt = LocalDateTime.now()
        memberShopItemRepository.save(MemberShopItem(member = member, shopItem = item))

        return PurchaseItemResponse(
            itemCode = item.itemCode,
            itemName = item.name,
            category = item.category,
            imageKey = item.imageKey,
            remainingPool = stats.availablePool,
            acquiredAt = acquiredAt,
        )
    }

    @Transactional
    fun equipItem(memberId: Long, itemCode: String): EquipItemResponse {
        val member = findMember(memberId)
        val item = findActiveOutfit(itemCode)

        check(isOwned(memberId, item)) {
            "보유하지 않은 아이템입니다."
        }

        member.equippedOutfitCode = item.itemCode
        member.equippedHatId = null
        member.equippedGlassesId = null
        member.equippedClothesId = null
        member.equippedBackgroundId = null

        return EquipItemResponse(equipped = member.toEquippedResponse())
    }

    @Transactional
    fun unequipOutfit(memberId: Long): EquipItemResponse {
        val member = findMember(memberId)
        member.equippedOutfitCode = null
        return EquipItemResponse(equipped = member.toEquippedResponse())
    }

    private fun findActiveOutfit(itemCode: String): ShopItem {
        val item = shopItemRepository.findByItemCode(itemCode)
            ?: throw IllegalArgumentException("아이템을 찾을 수 없습니다. itemCode=$itemCode")
        require(item.isActive && item.category == ShopItemCategory.OUTFIT) {
            "현재 판매 중인 의상이 아닙니다."
        }
        return item
    }

    private fun resolveOwnedOutfitCodes(memberId: Long): Set<String> {
        val purchased = memberShopItemRepository
            .findAllByMemberIdWithItem(memberId)
            .map { it.shopItem.itemCode }
            .toSet()

        val onboardingGifts = memberItemRepository
            .findAllByMemberId(memberId)
            .map { it.itemType.name }
            .toSet()

        return purchased + onboardingGifts + ShopOutfitCodes.DEFAULT
    }

    private fun isOwned(memberId: Long, item: ShopItem): Boolean {
        return item.itemCode in resolveOwnedOutfitCodes(memberId)
    }

    private fun findMember(memberId: Long) =
        memberRepository.findById(memberId)
            .orElseThrow { IllegalArgumentException("회원을 찾을 수 없습니다. id=$memberId") }

    private fun com.bifriends.domain.member.model.Member.toEquippedResponse() = EquippedItemsResponse(
        outfitCode = equippedOutfitCode,
    )
}
