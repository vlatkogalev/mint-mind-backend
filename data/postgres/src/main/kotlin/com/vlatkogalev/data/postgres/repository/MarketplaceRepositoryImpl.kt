package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.domain.marketplace.model.MarketplaceListing
import com.vlatkogalev.domain.marketplace.repository.MarketplaceRepository
import com.vlatkogalev.platform.database.tables.MarketplaceListingsTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class MarketplaceRepositoryImpl : MarketplaceRepository {
    override suspend fun upsertAll(listings: List<MarketplaceListing>) {
        if (listings.isEmpty()) return
        newSuspendedTransaction {
            listings.forEach { listing ->
                MarketplaceListingsTable.upsert(MarketplaceListingsTable.ebayItemId) {
                    it[id] = listing.id
                    it[ebayItemId] = listing.ebayItemId
                    it[title] = listing.title
                    it[price] = listing.price
                    it[currency] = listing.currency
                    it[condition] = listing.condition
                    it[listingUrl] = listing.listingUrl
                    it[imageUrl] = listing.imageUrl
                    it[buyingOptions] = listing.buyingOptions
                    it[expiresAt] = listing.expiresAt?.let { OffsetDateTime.ofInstant(it, ZoneOffset.UTC) }
                    it[lastSeenAt] = OffsetDateTime.ofInstant(listing.lastSeenAt, ZoneOffset.UTC)
                }
            }
        }
    }

    override suspend fun deleteNotSeenSince(threshold: Instant) {
        newSuspendedTransaction {
            MarketplaceListingsTable.deleteWhere {
                MarketplaceListingsTable.lastSeenAt less OffsetDateTime.ofInstant(threshold, ZoneOffset.UTC)
            }
        }
    }

    override suspend fun findPage(limit: Int, beforeTimestamp: Long?): List<MarketplaceListing> =
        newSuspendedTransaction {
            val query =
                MarketplaceListingsTable.selectAll().orderBy(MarketplaceListingsTable.lastSeenAt to SortOrder.DESC)
            val filtered = if (beforeTimestamp != null) {
                query.where {
                    MarketplaceListingsTable.lastSeenAt less OffsetDateTime.ofInstant(
                        Instant.ofEpochMilli(
                            beforeTimestamp
                        ), ZoneOffset.UTC
                    )
                }
            } else query
            filtered.limit(limit).map { it.toMarketplaceListing() }
        }

    override suspend fun getLatestFetchTime(): Instant? =
        newSuspendedTransaction {
            MarketplaceListingsTable.selectAll()
                .orderBy(MarketplaceListingsTable.lastSeenAt to SortOrder.DESC)
                .limit(1)
                .singleOrNull()
                ?.get(MarketplaceListingsTable.lastSeenAt)
                ?.toInstant()
        }

    private fun ResultRow.toMarketplaceListing() = MarketplaceListing(
        id = this[MarketplaceListingsTable.id],
        ebayItemId = this[MarketplaceListingsTable.ebayItemId],
        title = this[MarketplaceListingsTable.title],
        price = this[MarketplaceListingsTable.price],
        currency = this[MarketplaceListingsTable.currency],
        condition = this[MarketplaceListingsTable.condition],
        listingUrl = this[MarketplaceListingsTable.listingUrl],
        imageUrl = this[MarketplaceListingsTable.imageUrl],
        buyingOptions = this[MarketplaceListingsTable.buyingOptions],
        expiresAt = this[MarketplaceListingsTable.expiresAt]?.toInstant(),
        lastSeenAt = this[MarketplaceListingsTable.lastSeenAt].toInstant(),
    )
}
