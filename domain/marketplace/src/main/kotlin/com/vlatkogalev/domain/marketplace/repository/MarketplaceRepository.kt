package com.vlatkogalev.domain.marketplace.repository

import com.vlatkogalev.domain.marketplace.model.MarketplaceListing
import java.time.Instant

interface MarketplaceRepository {
    fun upsertAll(listings: List<MarketplaceListing>)

    fun deleteNotSeenSince(threshold: Instant)

    fun findPage(limit: Int, beforeTimestamp: Long?): List<MarketplaceListing>

    fun getLatestFetchTime(): Instant?
}
