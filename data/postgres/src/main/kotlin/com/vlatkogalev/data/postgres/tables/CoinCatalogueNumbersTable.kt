package com.vlatkogalev.data.postgres.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID

object CoinCatalogueNumbersTable : Table("coin_catalogue_numbers") {
    val id = javaUUID("id")
    val coinId = javaUUID("coin_id")
    val catalogueName = text("catalogue_name")
    val number = text("number").nullable()
    val confidence = varchar("confidence", 16)

    override val primaryKey = PrimaryKey(id)
}
