package com.vlatkogalev.data.postgres.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object CoinSetsTable : Table("coin_sets") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id")
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}
