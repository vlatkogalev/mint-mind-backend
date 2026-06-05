package com.vlatkogalev.data.postgres.entities

import java.time.Instant
import java.util.UUID

data class PasswordResetTokenRecord(
    val userId: UUID,
    val token: String,
    val expiresAt: Instant,
)