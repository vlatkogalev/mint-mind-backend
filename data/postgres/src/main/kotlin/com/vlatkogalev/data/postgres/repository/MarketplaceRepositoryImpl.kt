package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.tables.MarketplaceListingsTable
import com.vlatkogalev.domain.marketplace.model.MarketplaceListing
import com.vlatkogalev.domain.marketplace.repository.MarketplaceRepository
import com.vlatkogalev.platform.database.dbQuery
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.*
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class MarketplaceRepositoryImpl(
    private val database: R2dbcDatabase,
) : MarketplaceRepository {

    override suspend fun upsertAll(listings: List<MarketplaceListing>) =
        dbQuery(database) {
            val incomingIds = listings.map { it.ebayItemId }
            val existingIds = MarketplaceListingsTable
                .select(MarketplaceListingsTable.ebayItemId)
                .where { MarketplaceListingsTable.ebayItemId inList incomingIds }
                .toList()
                .map { it[MarketplaceListingsTable.ebayItemId] }
                .toSet()

            listings.forEach { listing ->
                if (listing.ebayItemId in existingIds) {
                    MarketplaceListingsTable.update({
                        MarketplaceListingsTable.ebayItemId eq listing.ebayItemId
                    }) {
                        it[title] = listing.title
                        it[price] = listing.price
                        it[currency] = listing.currency
                        it[condition] = listing.condition
                        it[listingUrl] = listing.listingUrl
                        it[imageUrl] = listing.imageUrl
                        it[buyingOptions] = listing.buyingOptions
                        it[expiresAt] = listing.expiresAt?.let { e -> OffsetDateTime.ofInstant(e, ZoneOffset.UTC) }
                        it[lastSeenAt] = OffsetDateTime.ofInstant(listing.lastSeenAt, ZoneOffset.UTC)
                    }
                } else {
                    MarketplaceListingsTable.insert {
                        it[id] = listing.id
                        it[ebayItemId] = listing.ebayItemId
                        it[title] = listing.title
                        it[price] = listing.price
                        it[currency] = listing.currency
                        it[condition] = listing.condition
                        it[listingUrl] = listing.listingUrl
                        it[imageUrl] = listing.imageUrl
                        it[buyingOptions] = listing.buyingOptions
                        it[expiresAt] = listing.expiresAt?.let { e -> OffsetDateTime.ofInstant(e, ZoneOffset.UTC) }
                        it[lastSeenAt] = OffsetDateTime.ofInstant(listing.lastSeenAt, ZoneOffset.UTC)
                    }
                }
            }
        }

    override suspend fun deleteNotSeenSince(threshold: Instant) =
        dbQuery(database) {
            MarketplaceListingsTable.deleteWhere {
                MarketplaceListingsTable.lastSeenAt less OffsetDateTime.ofInstant(threshold, ZoneOffset.UTC)
            }
            Unit
        }

    override suspend fun findPage(limit: Int, beforeTimestamp: Long?): List<MarketplaceListing> =
        dbQuery(database) {
            var query = MarketplaceListingsTable
                .selectAll()
                .orderBy(MarketplaceListingsTable.lastSeenAt to SortOrder.DESC)

            beforeTimestamp?.let {
                query = query.andWhere {
                    MarketplaceListingsTable.lastSeenAt less
                        OffsetDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC)
                }
            }

            query.limit(limit.coerceIn(1, 100)).toList().map { it.toMarketplaceListing() }
        }

    override suspend fun getLatestFetchTime(): Instant? =
        dbQuery(database) {
            MarketplaceListingsTable
                .selectAll()
                .orderBy(MarketplaceListingsTable.lastSeenAt to SortOrder.DESC)
                .limit(1)
                .toList()
                .firstOrNull()
                ?.get(MarketplaceListingsTable.lastSeenAt)
                ?.toInstant()
        }

    private fun ResultRow.toMarketplaceListing(): MarketplaceListing =
        MarketplaceListing(
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
