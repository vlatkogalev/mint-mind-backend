package com.vlatkogalev.domain.user.service

import java.util.UUID

interface UserTokenProvider {
    suspend fun createAccessToken(userId: UUID, isAnonymous: Boolean): String
    suspend fun generateRefreshToken(userId: UUID): String
    suspend fun accessTokenExpiresInSeconds(): Long
    suspend fun refreshTokenExpiresInSeconds(): Long
}
