package com.vlatkogalev.domain.marketplace.model

data class MarketplaceListingsPage(
    val listings: List<MarketplaceListing>,
    val nextCursor: Long?,
)
