package com.vlatkogalev.domain.user.service

interface UserTokenProvider {
    fun createAccessToken(userId: Long, email: String): String
    fun generateRefreshToken(): String
    fun accessTokenExpiresInSeconds(): Long
    fun refreshTokenExpiresInSeconds(): Long
}