package com.vlatkogalev.app.jobs

import com.vlatkogalev.data.ebay.EbayMarketplaceFetcher
import com.vlatkogalev.domain.marketplace.repository.MarketplaceRepository
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Fetches live eBay coin listings and persists them to the database.
 */
class EbayListingsJob(
    private val fetcher: EbayMarketplaceFetcher,
    private val repository: MarketplaceRepository,
    private val pagesToFetch: Int = 5,
) {
    private val log = LoggerFactory.getLogger(EbayListingsJob::class.java)

    fun run() {
        log.info("EbayListingsJob: starting fetch of up to {} pages", pagesToFetch)

        val runStartedAt = Instant.now()
        val listings = try {
            fetcher.fetchListings(pagesToFetch)
        } catch (ex: Exception) {
            log.error("EbayListingsJob: fetch failed, skipping database update", ex)
            return
        }

        if (listings.isEmpty()) {
            log.warn("EbayListingsJob: fetcher returned no listings")
            return
        }

        try {
            repository.upsertAll(listings)
            log.info("EbayListingsJob: upserted {} listings", listings.size)

            try {
                repository.deleteNotSeenSince(runStartedAt)
                log.info("EbayListingsJob: deleted stale listings")
            } catch (ex: Exception) {
                log.warn("EbayListingsJob: failed to delete stale listings", ex)
            }
        } catch (ex: Exception) {
            log.error("EbayListingsJob: database update failed", ex)
        }
    }
}
