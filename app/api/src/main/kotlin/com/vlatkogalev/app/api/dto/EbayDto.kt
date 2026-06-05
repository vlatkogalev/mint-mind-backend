package com.vlatkogalev.app.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class MarketplaceListingResponse(
    val id: String,
    val ebayItemId: String,
    val title: String,
    val price: String,
    val currency: String,
    val condition: String?,
    val listingUrl: String,
    val imageUrl: String?,
    val buyingOptions: List<String>,
    val expiresAt: String?,
    val timestamp: Long,
)

@Serializable
data class MarketplaceListingsResponse(
    val listings: List<MarketplaceListingResponse>,
    val nextCursor: Long?,
)

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
