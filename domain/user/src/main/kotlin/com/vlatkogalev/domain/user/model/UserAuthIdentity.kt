package com.vlatkogalev.domain.user.model

import java.time.Instant
import java.util.UUID

data class UserAuthIdentity(
    val id: UUID,
    val userId: UUID,
    val authType: AuthType,
    val email: String?,
    val passwordHash: String?,
    val createdAt: Instant,
)