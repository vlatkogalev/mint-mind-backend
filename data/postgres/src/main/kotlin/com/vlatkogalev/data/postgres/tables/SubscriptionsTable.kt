package com.vlatkogalev.data.postgres.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object SubscriptionsTable : Table("subscriptions") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id").uniqueIndex().references(UsersTable.id)
    val rcCustomerId = varchar("rc_customer_id", 255).uniqueIndex()
    val plan = varchar("plan", 32)
    val status = varchar("status", 32)
    val expiresAt = timestampWithTimeZone("expires_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
