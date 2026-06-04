package com.bifriends.domain.shop.dto

import com.bifriends.domain.shop.model.ShopItem
import com.bifriends.domain.shop.model.ShopItemCategory
import java.time.LocalDateTime

// ── 상점 아이템 목록 조회 ──────────────────────────────────────────────────────

data class ShopItemResponse(
    /** FE 연동용 고정 ID (예: GIFT_1, OUTFIT_DEFAULT) */
    val itemCode: String,
    val name: String,
    val category: ShopItemCategory,
    val price: Int,
    val imageKey: String,
    /** 이미 보유(기본·온보딩 선물·구매 완료)이면 true */
    val owned: Boolean,
) {
    companion object {
        fun from(item: ShopItem, owned: Boolean) = ShopItemResponse(
            itemCode = item.itemCode,
            name = item.name,
            category = item.category,
            price = item.price,
            imageKey = item.imageKey,
            owned = owned,
        )
    }
}

data class ShopItemListResponse(
    val availablePool: Int,
    val items: List<ShopItemResponse>,
)

// ── 보유 아이템 목록 (나의 서랍) ───────────────────────────────────────────────

data class OwnedShopItemResponse(
    val itemCode: String,
    val name: String,
    val category: ShopItemCategory,
    val imageKey: String,
    val acquiredAt: LocalDateTime?,
)

data class MyShopItemsResponse(
    val items: List<OwnedShopItemResponse>,
    val equipped: EquippedItemsResponse,
)

// ── 착용 아이템 ────────────────────────────────────────────────────────────────

data class EquippedItemsResponse(
    /** 착용 중인 전체 의상 코드 */
    val outfitCode: String? = null,
)

// ── 아이템 구매 ────────────────────────────────────────────────────────────────

data class PurchaseItemResponse(
    val itemCode: String,
    val itemName: String,
    val category: ShopItemCategory,
    val imageKey: String,
    val remainingPool: Int,
    val acquiredAt: LocalDateTime?,
)

// ── 아이템 착용/해제 ───────────────────────────────────────────────────────────

data class EquipItemResponse(
    val equipped: EquippedItemsResponse,
)
