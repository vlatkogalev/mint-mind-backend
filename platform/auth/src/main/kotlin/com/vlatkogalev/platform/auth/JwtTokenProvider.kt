package com.vlatkogalev.platform.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.vlatkogalev.domain.user.service.UserTokenProvider
import com.vlatkogalev.platform.core.config.AuthConfig
import com.vlatkogalev.platform.core.config.loadAuthConfig
import java.util.Date
import java.util.UUID

class JwtTokenProvider(
    private val config: AuthConfig = loadAuthConfig(),
) : UserTokenProvider {
    private val algorithm = Algorithm.HMAC256(config.secret)

    override suspend fun createAccessToken(userId: UUID, isAnonymous: Boolean): String {
        val nowMillis = System.currentTimeMillis()
        val expiryMillis = nowMillis + config.accessTokenTtlSeconds * 1000

        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId.toString())
            .withClaim("anonymous", isAnonymous)
            .withClaim("type", "access")
            .withIssuedAt(Date(nowMillis))
            .withExpiresAt(Date(expiryMillis))
            .sign(algorithm)
    }

    override suspend fun generateRefreshToken(userId: UUID): String = "$userId:${UUID.randomUUID()}"

    override suspend fun accessTokenExpiresInSeconds(): Long = config.accessTokenTtlSeconds

    override suspend fun refreshTokenExpiresInSeconds(): Long = config.refreshTokenTtlSeconds
}
