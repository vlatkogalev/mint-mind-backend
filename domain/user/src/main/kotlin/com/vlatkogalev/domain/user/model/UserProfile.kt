package com.vlatkogalev.domain.user.model

import java.util.UUID

data class UserProfile(
    val id: UUID,
    val userId: UUID,
    val firstName: String,
    val lastName: String,
    val avatarUrl: String?,
)