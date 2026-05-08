package com.vlatkogalev.platform.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.vlatkogalev.platform.core.config.AuthConfig
import com.vlatkogalev.platform.core.config.loadAuthConfig
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun JWTPrincipal.userIdOrNull(): String? = payload.subject

fun Application.configureAuth(config: AuthConfig = loadAuthConfig()) {
    install(Authentication) {
        jwt("jwt-auth") {
            realm = config.realm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(config.secret))
                    .withIssuer(config.issuer)
                    .withAudience(config.audience)
                    .build(),
            )
            validate { credential ->
                val hasAudience = credential.payload.audience.contains(config.audience)
                val hasSubject = credential.payload.subject != null
                if (hasAudience && hasSubject) JWTPrincipal(credential.payload) else null
            }
        }
    }
}
