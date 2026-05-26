package com.vlatkogalev.app.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class SoldListingResponse(
    val title: String,
    val soldPrice: Double,
    val currency: String,
    val soldAt: String,
    val condition: String?,
    val listingUrl: String,
    val imageUrl: String?,
)

@Serializable
data class PriceRangeResponse(
    val low: Double,
    val high: Double,
    val median: Double,
    val mean: Double,
    val sampleSize: Int,
)

@Serializable
data class CoinPricingResponse(
    val query: String,
    val recentSales: List<SoldListingResponse>,
    val priceRange: PriceRangeResponse?,
    val source: String,
    val fetchedAt: String,
)