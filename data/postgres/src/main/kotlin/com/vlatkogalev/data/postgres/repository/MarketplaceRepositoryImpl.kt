package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.daos.MarketplaceQueries
import com.vlatkogalev.data.postgres.entities.MarketplaceListingRecord
import com.vlatkogalev.domain.marketplace.model.MarketplaceListing
import com.vlatkogalev.domain.marketplace.repository.MarketplaceRepository
import java.time.Instant

class MarketplaceRepositoryImpl(
    private val queries: MarketplaceQueries,
) : MarketplaceRepository {
    override fun upsertAll(listings: List<MarketplaceListing>) {
        val records = listings.map { listing ->
            MarketplaceListingRecord(
                id = listing.id,
                ebayItemId = listing.ebayItemId,
                title = listing.title,
                price = listing.price,
                currency = listing.currency,
                condition = listing.condition,
                listingUrl = listing.listingUrl,
                imageUrl = listing.imageUrl,
                buyingOptions = listing.buyingOptions,
                expiresAt = listing.expiresAt,
                lastSeenAt = listing.lastSeenAt,
            )
        }
        queries.upsertAll(records)
    }

    override fun deleteNotSeenSince(threshold: Instant) {
        queries.deleteNotSeenSince(threshold)
    }

    override fun findPage(limit: Int, beforeTimestamp: Long?): List<MarketplaceListing> =
        queries.findPage(limit, beforeTimestamp).map { record ->
            MarketplaceListing(
                id = record.id,
                ebayItemId = record.ebayItemId,
                title = record.title,
                price = record.price,
                currency = record.currency,
                condition = record.condition,
                listingUrl = record.listingUrl,
                imageUrl = record.imageUrl,
                buyingOptions = record.buyingOptions,
                expiresAt = record.expiresAt,
                lastSeenAt = record.lastSeenAt,
            )
        }

    override fun getLatestFetchTime(): Instant? = queries.getLatestFetchTime()
}
