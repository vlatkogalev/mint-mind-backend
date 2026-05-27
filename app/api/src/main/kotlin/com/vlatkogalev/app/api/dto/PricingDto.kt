package com.vlatkogalev.app.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ActiveListingResponse(
    val title: String,
    val currentPrice: Double,
    val currency: String,
    val condition: String?,
    val listingUrl: String,
    val imageUrl: String?,
    val listingEndDate: String?,
    val buyingOptions: List<String>,
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
    val listings: List<ActiveListingResponse>,
    val priceRange: PriceRangeResponse?,
    val source: String,
    val fetchedAt: String,
)