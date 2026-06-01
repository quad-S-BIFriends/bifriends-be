package com.bifriends.domain.shop.repository

import com.bifriends.domain.shop.model.MemberShopItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MemberShopItemRepository : JpaRepository<MemberShopItem, Long> {

    /** 회원이 보유한 아이템 전체 조회 (shopItem fetch join) */
    @Query("SELECT msi FROM MemberShopItem msi JOIN FETCH msi.shopItem WHERE msi.member.id = :memberId")
    fun findAllByMemberIdWithItem(memberId: Long): List<MemberShopItem>

    /** 회원이 특정 아이템을 이미 보유했는지 확인 */
    fun existsByMemberIdAndShopItemId(memberId: Long, shopItemId: Long): Boolean
    fun deleteAllByMemberIdAndShopItemIdIn(memberId: Long, ids: List<Long>)
    fun deleteAllByMemberId(memberId: Long)
}
