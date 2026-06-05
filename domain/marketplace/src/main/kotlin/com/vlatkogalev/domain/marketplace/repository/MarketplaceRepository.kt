package com.vlatkogalev.domain.marketplace.repository

import com.vlatkogalev.domain.marketplace.model.MarketplaceListing
import java.time.Instant

interface MarketplaceRepository {
    suspend fun upsertAll(listings: List<MarketplaceListing>)
    suspend fun deleteNotSeenSince(threshold: Instant)
    suspend fun findPage(limit: Int, beforeTimestamp: Long?): List<MarketplaceListing>
    suspend fun getLatestFetchTime(): Instant?
}
