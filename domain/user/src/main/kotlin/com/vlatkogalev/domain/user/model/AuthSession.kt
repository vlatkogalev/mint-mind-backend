package com.vlatkogalev.domain.user.model

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresInSeconds: Long,
    val refreshTokenExpiresInSeconds: Long,
    val user: User,
)
