package com.vlatkogalev.domain.user.model

data class LoginSession(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresInSeconds: Long,
    val refreshTokenExpiresInSeconds: Long,
)