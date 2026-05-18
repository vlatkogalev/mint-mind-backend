package com.vlatkogalev.domain.coin.model

data class CoinCollectionStats(
    val totalCoins: Int,
    val totalIssuers: Int,
    val estimatedTotalValueMeanUsd: Double,
    val highlights: CollectionHighlights,
)

data class CollectionHighlights(
    val mostValuable: Coin?,
    val mostAncient: Coin?,
    val rarest: Coin?,
)
