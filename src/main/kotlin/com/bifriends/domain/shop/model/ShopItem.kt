package com.bifriends.domain.shop.model

import jakarta.persistence.*

/**
 * 상점 아이템 카탈로그.
 * 실제 판매 아이템 목록은 shop_seed.sql 로 초기 데이터를 적재한다.
 */
@Entity
@Table(name = "shop_items")
class ShopItem(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** 아이템 이름 */
    @Column(nullable = false)
    val name: String,

    /** 카테고리 (HAT / GLASSES / CLOTHES / BACKGROUND) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val category: ShopItemCategory,

    /** 구매 가격 (풀) */
    @Column(nullable = false)
    val price: Int,

    /**
     * FE에서 에셋 로드에 사용하는 키.
     * 예) "hat_cap_blue", "background_space"
     */
    @Column(nullable = false, unique = true)
    val imageKey: String,

    /** false = 시즌 종료·비활성화 아이템 (상점 목록에서 숨김) */
    @Column(nullable = false)
    val isActive: Boolean = true,
)
