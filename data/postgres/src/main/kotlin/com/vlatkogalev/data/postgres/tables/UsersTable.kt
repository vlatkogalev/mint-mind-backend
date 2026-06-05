package com.vlatkogalev.data.postgres.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object UsersTable : Table("users") {
    val id = javaUUID("id")
    val email = varchar("email", 320).nullable()
    val passwordHash = varchar("password_hash", 512).nullable()
    val emailVerified = bool("email_verified")
    val verificationToken = varchar("verification_token", 128).nullable()
    val verificationEmailSentAt = timestampWithTimeZone("verification_email_sent_at").nullable()
    val refreshTokenHash = varchar("refresh_token_hash", 512).nullable()
    val isAnonymous = bool("is_anonymous")
    val upgradedAt = timestampWithTimeZone("upgraded_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
