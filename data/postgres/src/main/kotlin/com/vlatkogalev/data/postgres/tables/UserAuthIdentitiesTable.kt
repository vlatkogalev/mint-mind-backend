package com.vlatkogalev.data.postgres.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object UserAuthIdentitiesTable : Table("user_auth_identities") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id").references(UsersTable.id)
    val authType = varchar("auth_type", 32)
    val email = varchar("email", 320).nullable()
    val passwordHash = varchar("password_hash", 512).nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}
