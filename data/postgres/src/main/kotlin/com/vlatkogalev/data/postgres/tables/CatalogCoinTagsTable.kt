package com.vlatkogalev.data.postgres.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object CatalogCoinTagsTable : Table("catalog_coin_tags") {
    val id = javaUUID("id")
    val catalogCoinId = javaUUID("catalog_coin_id")
    val tag = text("tag")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}
