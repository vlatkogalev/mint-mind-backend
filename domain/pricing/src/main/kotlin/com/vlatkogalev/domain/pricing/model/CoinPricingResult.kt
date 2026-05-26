package com.vlatkogalev.domain.pricing.model

import java.time.Instant

data class CoinPricingResult(
    val query: String,
    val recentSales: List<SoldListing>,
    val priceRange: PriceRange?,
    val source: String,
    val fetchedAt: Instant,
)