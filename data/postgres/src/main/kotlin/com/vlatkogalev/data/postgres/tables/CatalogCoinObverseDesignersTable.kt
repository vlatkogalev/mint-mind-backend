package com.vlatkogalev.data.postgres.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object CatalogCoinObverseDesignersTable : Table("catalog_coin_obverse_designers") {
    val id = javaUUID("id")
    val catalogCoinId = javaUUID("catalog_coin_id")
    val designer = text("designer")
    val position = integer("position")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}
