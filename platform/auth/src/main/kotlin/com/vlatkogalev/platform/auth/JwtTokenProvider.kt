package com.vlatkogalev.platform.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.vlatkogalev.domain.user.service.UserTokenProvider
import com.vlatkogalev.platform.core.config.AuthConfig
import com.vlatkogalev.platform.core.config.loadAuthConfig
import java.util.Date

class JwtTokenProvider(
    private val config: AuthConfig = loadAuthConfig(),
) : UserTokenProvider {
    private val algorithm = Algorithm.HMAC256(config.secret)

    override fun createAccessToken(userId: Long, email: String): String {
        val nowMillis = System.currentTimeMillis()
        val expiryMillis = nowMillis + config.accessTokenTtlSeconds * 1000

        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withClaim("type", "access")
            .withIssuedAt(Date(nowMillis))
            .withExpiresAt(Date(expiryMillis))
            .sign(algorithm)
    }

    override fun generateRefreshToken(): String = java.util.UUID.randomUUID().toString()

    override fun accessTokenExpiresInSeconds(): Long = config.accessTokenTtlSeconds

    override fun refreshTokenExpiresInSeconds(): Long = config.refreshTokenTtlSeconds
}
