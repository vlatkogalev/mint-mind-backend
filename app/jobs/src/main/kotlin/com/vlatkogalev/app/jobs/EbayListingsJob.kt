package com.vlatkogalev.app.jobs

import com.vlatkogalev.data.ebay.EbayMarketplaceFetcher
import com.vlatkogalev.data.ebay.EbayItemSummary
import com.vlatkogalev.domain.marketplace.model.MarketplaceListing
import com.vlatkogalev.domain.marketplace.repository.MarketplaceRepository
import java.time.Instant
import java.util.UUID

class EbayListingsJob(
    private val fetcher: EbayMarketplaceFetcher,
    private val repository: MarketplaceRepository,
    private val pagesToFetch: Int = 5,
) {
    suspend fun run() {
        val runStartedAt = Instant.now()
        val items = fetcher.fetchListings(pagesToFetch)

        if (items.isEmpty()) return

        val listings = items.mapNotNull { it.toMarketplaceListing(runStartedAt) }
        repository.upsertAll(listings)
        repository.deleteNotSeenSince(runStartedAt)
    }
}

private fun EbayItemSummary.toMarketplaceListing(lastSeenAt: Instant): MarketplaceListing? {
    val itemId = itemId ?: return null
    val title = title ?: return null
    return MarketplaceListing(
        id = UUID.randomUUID(),
        ebayItemId = itemId,
        title = title,
        price = price?.value ?: "0.00",
        currency = price?.currency ?: "USD",
        condition = condition,
        listingUrl = itemWebUrl ?: "",
        imageUrl = image?.imageUrl,
        buyingOptions = buyingOptions ?: emptyList(),
        expiresAt = itemEndDate?.let { runCatching { Instant.parse(it) }.getOrNull() },
        lastSeenAt = lastSeenAt,
    )
}
