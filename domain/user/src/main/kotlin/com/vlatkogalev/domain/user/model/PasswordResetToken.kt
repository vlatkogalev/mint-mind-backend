package com.vlatkogalev.domain.user.model

import java.time.Instant
import java.util.UUID

data class PasswordResetToken(
    val userId: UUID,
    val token: String,
    val expiresAt: Instant,
)
