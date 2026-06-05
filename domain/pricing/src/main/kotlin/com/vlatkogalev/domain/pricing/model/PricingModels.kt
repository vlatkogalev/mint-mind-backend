package com.vlatkogalev.domain.pricing.model

import java.time.Instant

data class ActiveListing(
    val title: String,
    val currentPrice: Double,
    val currency: String,
    val condition: String?,
    val listingUrl: String,
    val imageUrl: String?,
    val thumbnailUrl: String?,
    val listingEndDate: Instant?,
    val buyingOptions: List<String>,
)

data class PriceRange(
    val low: Double,
    val high: Double,
    val median: Double,
    val mean: Double,
    val sampleSize: Int,
)

data class CoinPricingResult(
    val query: String,
    val listings: List<ActiveListing>,
    val priceRange: PriceRange?,
    val source: String,
    val fetchedAt: Instant,
)
