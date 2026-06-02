package com.vlatkogalev.platform.core.config

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
)

fun loadDatabaseConfig(env: Map<String, String> = System.getenv()): DatabaseConfig =
    DatabaseConfig(
        url = env["DB_URL"] ?: error("DB_URL environment variable is required"),
        user = env["DB_USER"] ?: error("DB_USER environment variable is required"),
        password = env["DB_PASSWORD"] ?: error("DB_PASSWORD environment variable is required"),
    )
