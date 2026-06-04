package com.bifriends.domain.shop.model

/** 상점·온보딩 공통 의상 코드 */
object ShopOutfitCodes {
    const val DEFAULT = "OUTFIT_DEFAULT"
    const val STUDYING = "OUTFIT_STUDYING"

    fun isAlwaysOwned(code: String): Boolean = code == DEFAULT
}
