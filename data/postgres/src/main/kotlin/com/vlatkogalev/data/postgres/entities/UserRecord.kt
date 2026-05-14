package com.vlatkogalev.data.postgres.entities

import java.util.UUID
import java.time.Instant

data class UserRecord(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val emailVerified: Boolean,
    val verificationToken: String?,
    val verificationEmailSentAt: Instant?,
    val refreshTokenHash: String?,
    val profileId: UUID?,
    val firstName: String?,
    val lastName: String?,
    val avatarUrl: String?,
)
