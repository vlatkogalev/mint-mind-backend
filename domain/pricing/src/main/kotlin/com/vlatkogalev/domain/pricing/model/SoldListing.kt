package com.vlatkogalev.domain.pricing.model

import java.time.Instant

data class SoldListing(
    val title: String,
    val soldPrice: Double,
    val currency: String,
    val soldAt: Instant,
    val condition: String?,
    val listingUrl: String,
    val imageUrl: String?,
)