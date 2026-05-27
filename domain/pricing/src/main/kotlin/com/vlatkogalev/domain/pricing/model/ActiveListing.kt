package com.vlatkogalev.domain.pricing.model

import java.time.Instant

data class ActiveListing(
    val title: String,
    val currentPrice: Double,
    val currency: String,
    val condition: String?,
    val listingUrl: String,
    val imageUrl: String?,
    val listingEndDate: Instant?,
    val buyingOptions: List<String>,
)