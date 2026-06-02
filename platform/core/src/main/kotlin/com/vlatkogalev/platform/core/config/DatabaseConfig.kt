package com.vlatkogalev.platform.core.config

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
)

fun loadDatabaseConfig(env: Map<String, String> = System.getenv()): DatabaseConfig =
    DatabaseConfig(
        url = requiredEnv(env, "DB_URL"),
        user = requiredEnv(env, "DB_USER"),
        password = requiredEnv(env, "DB_PASSWORD"),
    )
