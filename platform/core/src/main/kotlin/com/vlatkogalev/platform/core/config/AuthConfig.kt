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
        issuer = env["AUTH_ISSUER"] ?: "mintmind-service",
        audience = env["AUTH_AUDIENCE"] ?: "mintmind-users",
        realm = env["AUTH_REALM"] ?: "mintmind-api",
        secret = env["AUTH_SECRET"] ?: "mintmind-auth_secret",
        accessTokenTtlSeconds = env["AUTH_ACCESS_TOKEN_TTL_SECONDS"]?.toLongOrNull() ?: 3600,
        refreshTokenTtlSeconds = env["AUTH_REFRESH_TOKEN_TTL_SECONDS"]?.toLongOrNull() ?: 2_592_000,
    )
