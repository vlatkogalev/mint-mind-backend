package com.vlatkogalev.domain.coin.model

data class CoinCollectionStats(
    val totalCoins: Int,
    val estimatedTotalValueLowUsd: Double,
    val estimatedTotalValueHighUsd: Double,
    val byCountry: Map<String, Int>,
    val byYear: Map<Int, Int>,
)