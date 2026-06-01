package com.vlatkogalev.app.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class MarketplaceListingResponse(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val price: String,
    val currency: String,
    val itemWebUrl: String,
    val condition: String?,
    val buyingOptions: List<String>,
    val expiresAt: Long?,
    val timestamp: Long,
)

@Serializable
data class MarketplaceListingsResponse(
    val listings: List<MarketplaceListingResponse>,
    val nextCursor: Long?,
)
