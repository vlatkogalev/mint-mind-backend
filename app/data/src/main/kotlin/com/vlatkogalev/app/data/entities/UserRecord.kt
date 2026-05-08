package com.vlatkogalev.app.data.entities

data class UserRecord(
    val id: Long,
    val email: String,
    val fullName: String,
    val passwordHash: String,
)
