package com.bifriends.domain.shop.model

import com.bifriends.domain.member.model.Member
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 회원이 구매한 상점 아이템 소유 이력.
 *
 * 회원이 동일 아이템을 중복 구매하지 않도록 (member_id, shop_item_id) unique 제약.
 */
@Entity
@Table(
    name = "member_shop_items",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_member_shop_item",
        columnNames = ["member_id", "shop_item_id"]
    )],
    indexes = [Index(name = "idx_member_shop_items_member", columnList = "member_id")]
)
class MemberShopItem(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_item_id", nullable = false)
    val shopItem: ShopItem,

    @Column(nullable = false, updatable = false)
    val acquiredAt: LocalDateTime = LocalDateTime.now(),
)
