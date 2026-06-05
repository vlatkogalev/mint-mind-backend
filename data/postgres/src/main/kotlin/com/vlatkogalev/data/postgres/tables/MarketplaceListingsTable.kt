package com.vlatkogalev.data.postgres.tables

import com.vlatkogalev.data.postgres.columns.PostgresTextArrayColumnType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object MarketplaceListingsTable : Table("marketplace_listings") {
    val id = javaUUID("id")
    val ebayItemId = text("ebay_item_id")
    val title = text("title")
    val price = text("price")
    val currency = varchar("currency", 10)
    val condition = text("condition").nullable()
    val listingUrl = text("listing_url")
    val imageUrl = text("image_url").nullable()
    val buyingOptions = registerColumn("buying_options", PostgresTextArrayColumnType())
    val expiresAt = timestampWithTimeZone("expires_at").nullable()
    val lastSeenAt = timestampWithTimeZone("last_seen_at")

    override val primaryKey = PrimaryKey(id)
}
