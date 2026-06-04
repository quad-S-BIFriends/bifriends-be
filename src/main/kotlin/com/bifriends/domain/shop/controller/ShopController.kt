package com.bifriends.domain.shop.controller

import com.bifriends.domain.shop.dto.*
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

    @GetMapping("/items")
    fun getShopItems(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<ShopItemListResponse> {
        return ResponseEntity.ok(shopService.getShopItems(extractMemberId(token)))
    }

    @GetMapping("/my-items")
    fun getMyItems(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<MyShopItemsResponse> {
        return ResponseEntity.ok(shopService.getMyItems(extractMemberId(token)))
    }

    @PostMapping("/items/{itemCode}/purchase")
    fun purchaseItem(
        @RequestHeader("Authorization") token: String,
        @PathVariable itemCode: String,
    ): ResponseEntity<PurchaseItemResponse> {
        return ResponseEntity.ok(shopService.purchaseItem(extractMemberId(token), itemCode))
    }

    @PatchMapping("/items/{itemCode}/equip")
    fun equipItem(
        @RequestHeader("Authorization") token: String,
        @PathVariable itemCode: String,
    ): ResponseEntity<EquipItemResponse> {
        return ResponseEntity.ok(shopService.equipItem(extractMemberId(token), itemCode))
    }

    @DeleteMapping("/items/equip")
    fun unequipOutfit(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<EquipItemResponse> {
        return ResponseEntity.ok(shopService.unequipOutfit(extractMemberId(token)))
    }

    private fun extractMemberId(token: String): Long =
        jwtProvider.getMemberId(token.removePrefix("Bearer "))
}
