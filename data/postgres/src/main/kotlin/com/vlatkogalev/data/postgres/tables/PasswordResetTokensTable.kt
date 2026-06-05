package com.vlatkogalev.data.postgres.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object PasswordResetTokensTable : Table("password_reset_tokens") {
    val userId = javaUUID("user_id").references(UsersTable.id)
    val token = varchar("token", 128).uniqueIndex()
    val expiresAt = timestampWithTimeZone("expires_at")

    override val primaryKey = PrimaryKey(userId)
}
