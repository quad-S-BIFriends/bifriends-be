package com.bifriends.domain.shop.controller

import com.bifriends.domain.shop.dto.*
import com.bifriends.domain.shop.model.ShopItemCategory
import com.bifriends.domain.shop.service.ShopService
import com.bifriends.infrastructure.security.JwtProvider
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/shop")
class ShopController(
    private val shopService: ShopService,
    private val jwtProvider: JwtProvider,
) {

    /** HOM-09-01/03 — 상점 아이템 목록 (보유 여부 포함) */
    @GetMapping("/items")
    fun getShopItems(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<ShopItemListResponse> {
        return ResponseEntity.ok(shopService.getShopItems(extractMemberId(token)))
    }

    /** 나의 서랍 — 보유 아이템 + 착용 현황 */
    @GetMapping("/my-items")
    fun getMyItems(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<MyShopItemsResponse> {
        return ResponseEntity.ok(shopService.getMyItems(extractMemberId(token)))
    }

    /** HOM-09-03/04 — 아이템 구매 (풀 차감 + 소유권 추가) */
    @PostMapping("/items/{itemId}/purchase")
    fun purchaseItem(
        @RequestHeader("Authorization") token: String,
        @PathVariable itemId: Long,
    ): ResponseEntity<PurchaseItemResponse> {
        return ResponseEntity.ok(shopService.purchaseItem(extractMemberId(token), itemId))
    }

    /** HOM-09-05 — 아이템 착용 */
    @PatchMapping("/items/{itemId}/equip")
    fun equipItem(
        @RequestHeader("Authorization") token: String,
        @PathVariable itemId: Long,
    ): ResponseEntity<EquipItemResponse> {
        return ResponseEntity.ok(shopService.equipItem(extractMemberId(token), itemId))
    }

    /** 아이템 해제 (카테고리 전체 해제) */
    @DeleteMapping("/items/{category}/equip")
    fun unequipItem(
        @RequestHeader("Authorization") token: String,
        @PathVariable category: ShopItemCategory,
    ): ResponseEntity<EquipItemResponse> {
        return ResponseEntity.ok(shopService.unequipItem(extractMemberId(token), category))
    }

    private fun extractMemberId(token: String): Long =
        jwtProvider.getMemberId(token.removePrefix("Bearer "))
}
