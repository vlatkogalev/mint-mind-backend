package com.vlatkogalev.domain.user.model

import java.util.UUID
import java.time.Instant

data class UserAccount(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val emailVerified: Boolean,
    val verificationToken: String?,
    val verificationEmailSentAt: Instant?,
    val refreshTokenHash: String?,
    val profile: UserProfile?,
)