package com.vlatkogalev.platform.core.config

data class AuthConfig(
    val issuer: String,
    val audience: String,
    val realm: String,
    val secret: String,
    val accessTokenTtlSeconds: Long,
    val refreshTokenTtlSeconds: Long,
)

fun loadAuthConfig(env: Map<String, String> = System.getenv()): AuthConfig =
    AuthConfig(
        issuer = env["AUTH_ISSUER"] ?: "template-service",
        audience = env["AUTH_AUDIENCE"] ?: "template-users",
        realm = env["AUTH_REALM"] ?: "template-api",
        secret = env["AUTH_SECRET"] ?: "template-auth_secret",
        accessTokenTtlSeconds = env["AUTH_ACCESS_TOKEN_TTL_SECONDS"]?.toLongOrNull() ?: 3600,
        refreshTokenTtlSeconds = env["AUTH_REFRESH_TOKEN_TTL_SECONDS"]?.toLongOrNull() ?: 2_592_000,
    )
