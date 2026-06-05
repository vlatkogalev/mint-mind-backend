package com.vlatkogalev.data.postgres.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object ExternalCoinReferencesTable : Table("external_coin_references") {
    val id = javaUUID("id")
    val catalogCoinId = javaUUID("catalog_coin_id")
    val provider = varchar("provider", 64)
    val externalId = text("external_id")
    val externalUrl = text("external_url").nullable()
    val lastSyncedAt = timestampWithTimeZone("last_synced_at").nullable()
    val syncStatus = varchar("sync_status", 32).nullable()
    val syncError = text("sync_error").nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}
