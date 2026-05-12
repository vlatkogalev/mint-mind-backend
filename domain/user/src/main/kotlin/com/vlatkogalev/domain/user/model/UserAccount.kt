package com.vlatkogalev.domain.user.model

import java.util.UUID

data class UserAccount(
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val emailVerified: Boolean,
    val verificationToken: String?,
    val refreshTokenHash: String?,
    val profile: UserProfile?,
)