package com.vlatkogalev.app.data.entities

import java.time.Instant

data class PasswordResetTokenRecord(
    val userId: Long,
    val token: String,
    val expiresAt: Instant,
)