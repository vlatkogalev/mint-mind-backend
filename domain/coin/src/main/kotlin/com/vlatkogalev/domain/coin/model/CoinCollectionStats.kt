package com.vlatkogalev.domain.coin.model

data class CoinCollectionStats(
    val totalCoins: Int,
    val totalIssuers: Int,
    val estimatedTotalValueMean: Double,
    val highlights: CollectionHighlights,
)
