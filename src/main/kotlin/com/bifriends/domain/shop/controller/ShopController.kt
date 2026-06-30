package com.bifriends.domain.shop.controller

import com.bifriends.domain.shop.dto.*
import com.bifriends.domain.shop.service.ShopService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/shop")
class ShopController(
    private val shopService: ShopService,
) {

    @GetMapping("/items")
    fun getShopItems(
        @AuthenticationPrincipal memberId: Long,
    ): ResponseEntity<ShopItemListResponse> {
        return ResponseEntity.ok(shopService.getShopItems(memberId))
    }

    @GetMapping("/my-items")
    fun getMyItems(
        @AuthenticationPrincipal memberId: Long,
    ): ResponseEntity<MyShopItemsResponse> {
        return ResponseEntity.ok(shopService.getMyItems(memberId))
    }

    @PostMapping("/items/{itemCode}/purchase")
    fun purchaseItem(
        @AuthenticationPrincipal memberId: Long,
        @PathVariable itemCode: String,
    ): ResponseEntity<PurchaseItemResponse> {
        return ResponseEntity.ok(shopService.purchaseItem(memberId, itemCode))
    }

    @PatchMapping("/items/{itemCode}/equip")
    fun equipItem(
        @AuthenticationPrincipal memberId: Long,
        @PathVariable itemCode: String,
    ): ResponseEntity<EquipItemResponse> {
        return ResponseEntity.ok(shopService.equipItem(memberId, itemCode))
    }

    @DeleteMapping("/items/equip")
    fun unequipOutfit(
        @AuthenticationPrincipal memberId: Long,
    ): ResponseEntity<EquipItemResponse> {
        return ResponseEntity.ok(shopService.unequipOutfit(memberId))
    }

}
