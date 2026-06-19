package com.vlatkogalev.data.postgres.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object CatalogCoinCatalogueNumbersTable : Table("catalog_coin_catalogue_numbers") {
    val id = javaUUID("id")
    val catalogCoinId = javaUUID("catalog_coin_id")
    val catalogueName = text("catalogue_name")
    val number = text("number").nullable()
    val confidence = varchar("confidence", 16)
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}
