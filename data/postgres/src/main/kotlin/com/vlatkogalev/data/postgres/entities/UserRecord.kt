package com.vlatkogalev.data.postgres.entities

data class UserRecord(
    val id: Long,
    val email: String,
    val fullName: String,
    val passwordHash: String,
)
