package com.vlatkogalev.domain.user.model

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String?,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String?,
    val emailVerified: Boolean,
    val isAnonymous: Boolean,
    val upgradedAt: Instant?,
)
