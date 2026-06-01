package com.vlatkogalev.data.postgres.entities

import java.time.Instant
import java.util.UUID

data class MarketplaceListingRecord(
    val id: UUID,
    val ebayItemId: String,
    val title: String,
    val price: String,
    val currency: String,
    val condition: String?,
    val listingUrl: String,
    val imageUrl: String?,
    val buyingOptions: List<String>,
    val expiresAt: Instant?,
    val lastSeenAt: Instant,
)
