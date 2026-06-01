package com.bifriends.domain.shop.dto

import com.bifriends.domain.shop.model.ShopItem
import com.bifriends.domain.shop.model.ShopItemCategory
import java.time.LocalDateTime

// ── 상점 아이템 목록 조회 ──────────────────────────────────────────────────────

data class ShopItemResponse(
    val id: Long,
    val name: String,
    val category: ShopItemCategory,
    val price: Int,
    val imageKey: String,
    /** 이미 구매한 아이템이면 true */
    val owned: Boolean,
) {
    companion object {
        fun from(item: ShopItem, owned: Boolean) = ShopItemResponse(
            id = item.id,
            name = item.name,
            category = item.category,
            price = item.price,
            imageKey = item.imageKey,
            owned = owned,
        )
    }
}

data class ShopItemListResponse(
    /** 현재 보유 풀 (구매 가능 여부 판단용) */
    val availablePool: Int,
    val items: List<ShopItemResponse>,
)

// ── 보유 아이템 목록 (나의 서랍) ───────────────────────────────────────────────

data class OwnedShopItemResponse(
    val id: Long,
    val name: String,
    val category: ShopItemCategory,
    val imageKey: String,
    val acquiredAt: LocalDateTime,
)

data class MyShopItemsResponse(
    val items: List<OwnedShopItemResponse>,
    /** 카테고리별 현재 착용 아이템 ID */
    val equipped: EquippedItemsResponse,
)

// ── 착용 아이템 ────────────────────────────────────────────────────────────────

data class EquippedItemsResponse(
    val hatId: Long? = null,
    val glassesId: Long? = null,
    val clothesId: Long? = null,
    val backgroundId: Long? = null,
)

// ── 아이템 구매 ────────────────────────────────────────────────────────────────

data class PurchaseItemResponse(
    val itemId: Long,
    val itemName: String,
    val category: ShopItemCategory,
    val imageKey: String,
    /** 구매 후 남은 풀 */
    val remainingPool: Int,
    val acquiredAt: LocalDateTime,
)

// ── 아이템 착용/해제 ───────────────────────────────────────────────────────────

data class EquipItemResponse(
    val equipped: EquippedItemsResponse,
)
