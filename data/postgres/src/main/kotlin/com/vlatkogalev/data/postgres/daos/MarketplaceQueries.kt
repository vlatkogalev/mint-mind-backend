package com.vlatkogalev.data.postgres.daos

import com.vlatkogalev.data.postgres.entities.MarketplaceListingRecord
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

class MarketplaceQueries(
    private val dataSource: DataSource,
) {
    fun upsertAll(listings: List<MarketplaceListingRecord>) {
        if (listings.isEmpty()) return

        dataSource.connection.use { connection ->
            val database = connection.metaData.databaseProductName.lowercase()
            connection.prepareStatement(upsertSql(database)).use { statement ->
                listings.forEach { listing ->
                    statement.setObject(1, listing.id)
                    statement.setString(2, listing.ebayItemId)
                    statement.setString(3, listing.title)
                    statement.setString(4, listing.price)
                    statement.setString(5, listing.currency)
                    statement.setString(6, listing.condition)
                    statement.setString(7, listing.listingUrl)
                    statement.setString(8, listing.imageUrl)
                    bindBuyingOptions(statement, connection, database, 9, listing.buyingOptions)
                    statement.setObject(10, listing.expiresAt?.let { OffsetDateTime.ofInstant(it, ZoneOffset.UTC) })
                    statement.setObject(11, OffsetDateTime.ofInstant(listing.lastSeenAt, ZoneOffset.UTC))
                    statement.addBatch()
                }
                statement.executeBatch()
            }
        }
    }

    fun deleteNotSeenSince(threshold: Instant) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "DELETE FROM marketplace_listings WHERE last_seen_at < ?",
            ).use { statement ->
                statement.setObject(1, OffsetDateTime.ofInstant(threshold, ZoneOffset.UTC))
                statement.executeUpdate()
            }
        }
    }

    fun findPage(limit: Int, beforeTimestamp: Long?): List<MarketplaceListingRecord> =
        dataSource.connection.use { connection ->
            val sql = if (beforeTimestamp == null) {
                """
                SELECT ${columns()}
                FROM marketplace_listings
                ORDER BY last_seen_at DESC
                LIMIT ?
                """.trimIndent()
            } else {
                """
                SELECT ${columns()}
                FROM marketplace_listings
                WHERE extract(epoch FROM last_seen_at) * 1000 < ?
                ORDER BY last_seen_at DESC
                LIMIT ?
                """.trimIndent()
            }

            connection.prepareStatement(sql).use { statement ->
                var index = 1
                if (beforeTimestamp != null) {
                    statement.setLong(index++, beforeTimestamp)
                }
                statement.setInt(index, limit)
                statement.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) add(rs.toRecord())
                    }
                }
            }
        }

    fun getLatestFetchTime(): Instant? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "SELECT MAX(last_seen_at) AS max_last_seen_at FROM marketplace_listings",
            ).use { statement ->
                statement.executeQuery().use { rs ->
                    if (rs.next()) rs.getObject("max_last_seen_at", OffsetDateTime::class.java)?.toInstant()
                    else null
                }
            }
        }

    private fun upsertSql(database: String): String =
        if (database.contains("h2")) {
            """
            MERGE INTO marketplace_listings
                (id, ebay_item_id, title, price, currency, condition,
                 listing_url, image_url, buying_options, expires_at, last_seen_at)
            KEY (ebay_item_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        } else {
            """
            INSERT INTO marketplace_listings
                (id, ebay_item_id, title, price, currency, condition,
                 listing_url, image_url, buying_options, expires_at, last_seen_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (ebay_item_id)
            DO UPDATE SET
                title          = EXCLUDED.title,
                price          = EXCLUDED.price,
                condition      = EXCLUDED.condition,
                image_url      = EXCLUDED.image_url,
                buying_options = EXCLUDED.buying_options,
                expires_at     = EXCLUDED.expires_at,
                last_seen_at   = EXCLUDED.last_seen_at
            """.trimIndent()
        }

    private fun bindBuyingOptions(
        statement: PreparedStatement,
        connection: Connection,
        database: String,
        index: Int,
        buyingOptions: List<String>,
    ) {
        if (database.contains("h2")) {
            statement.setString(index, buyingOptions.joinToString(","))
            return
        }
        val array = connection.createArrayOf("text", buyingOptions.toTypedArray())
        statement.setArray(index, array)
        array.free()
    }

    private fun ResultSet.toRecord(): MarketplaceListingRecord =
        MarketplaceListingRecord(
            id = getObject("id", UUID::class.java),
            ebayItemId = getString("ebay_item_id"),
            title = getString("title"),
            price = getString("price"),
            currency = getString("currency"),
            condition = getString("condition"),
            listingUrl = getString("listing_url"),
            imageUrl = getString("image_url"),
            buyingOptions = extractBuyingOptions(getObject("buying_options")),
            expiresAt = getObject("expires_at", OffsetDateTime::class.java)?.toInstant(),
            lastSeenAt = getObject("last_seen_at", OffsetDateTime::class.java).toInstant(),
        )

    private fun extractBuyingOptions(value: Any?): List<String> = when (value) {
        null -> emptyList()
        is String -> value.split(",").filter { it.isNotBlank() }
        is java.sql.Array -> (value.array as? kotlin.Array<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        else -> emptyList()
    }

    private fun columns(): String =
        """
        id, ebay_item_id, title, price, currency, condition,
        listing_url, image_url, buying_options, expires_at, last_seen_at
        """.trimIndent()
}
