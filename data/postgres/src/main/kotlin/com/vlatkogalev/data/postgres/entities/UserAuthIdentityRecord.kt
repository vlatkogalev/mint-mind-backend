package com.vlatkogalev.data.postgres.entities

import java.time.Instant
import java.util.UUID

data class UserAuthIdentityRecord(
    val id: UUID,
    val userId: UUID,
    val authType: String,
    val email: String?,
    val passwordHash: String?,
    val createdAt: Instant,
)
