package com.vlatkogalev.data.postgres.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID

object ProfilesTable : Table("profiles") {
    val id = javaUUID("id")
    val userId = javaUUID("user_id").uniqueIndex().references(UsersTable.id)
    val firstName = varchar("first_name", 255)
    val lastName = varchar("last_name", 255)
    val avatarUrl = varchar("avatar_url", 2048).nullable()

    override val primaryKey = PrimaryKey(id)
}
