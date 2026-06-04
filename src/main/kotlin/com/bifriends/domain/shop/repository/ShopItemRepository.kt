package com.bifriends.domain.shop.repository

import com.bifriends.domain.shop.model.ShopItem
import com.bifriends.domain.shop.model.ShopItemCategory
import org.springframework.data.jpa.repository.JpaRepository

interface ShopItemRepository : JpaRepository<ShopItem, Long> {
    fun findAllByIsActiveTrueOrderByPriceAscIdAsc(): List<ShopItem>
    fun findByItemCode(itemCode: String): ShopItem?
    fun findAllByCategoryAndIsActiveTrue(category: ShopItemCategory): List<ShopItem>
}
