package com.vlatkogalev.data.postgres.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object AnonymousInstallationsTable : Table("anonymous_installations") {
    val installationId = varchar("installation_id", 255)
    val userId = javaUUID("user_id").references(UsersTable.id)
    val lastSeenAt = timestampWithTimeZone("last_seen_at")

    override val primaryKey = PrimaryKey(installationId)
}
