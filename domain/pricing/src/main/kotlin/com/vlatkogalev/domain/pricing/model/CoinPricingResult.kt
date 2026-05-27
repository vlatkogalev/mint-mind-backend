package com.vlatkogalev.domain.pricing.model

import java.time.Instant

/**
 * Pricing data fetched from eBay Browse API.
 *
 * Note: [listings] contains *active* listings (asking prices), not sold prices.
 * The eBay Browse API does not expose completed/sold item data.
 * [priceRange] is computed from current listing prices and should be
 * presented to users as a market price estimate, not a guaranteed sale value.
 */
data class CoinPricingResult(
    val query: String,
    val listings: List<ActiveListing>,
    val priceRange: PriceRange?,
    val source: String,
    val fetchedAt: Instant,
)