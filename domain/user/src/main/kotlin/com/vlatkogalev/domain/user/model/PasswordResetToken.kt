package com.vlatkogalev.domain.user.model

import java.time.Instant

data class PasswordResetToken(
    val userId: Long,
    val token: String,
    val expiresAt: Instant,
)