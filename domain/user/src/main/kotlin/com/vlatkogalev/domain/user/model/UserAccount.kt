package com.vlatkogalev.domain.user.model

data class UserAccount(
    val id: Long,
    val email: String,
    val fullName: String,
    val passwordHash: String,
)