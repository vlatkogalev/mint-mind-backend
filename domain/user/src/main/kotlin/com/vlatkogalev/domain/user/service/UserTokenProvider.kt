package com.vlatkogalev.domain.user.service

import java.util.UUID

interface UserTokenProvider {
    fun createAccessToken(userId: UUID, email: String): String
    fun generateRefreshToken(userId: UUID): String
    fun accessTokenExpiresInSeconds(): Long
    fun refreshTokenExpiresInSeconds(): Long
}