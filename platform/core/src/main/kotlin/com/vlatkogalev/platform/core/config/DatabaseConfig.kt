package com.vlatkogalev.platform.core.config

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
) {
    val jdbcUrl: String get() = url
    val r2dbcUrl: String get() = url.replaceFirst("jdbc:", "r2dbc:")
}

fun loadDatabaseConfig(env: Map<String, String> = System.getenv()): DatabaseConfig =
    DatabaseConfig(
        url = env["DB_URL"] ?: error("DB_URL environment variable is required"),
        user = env["DB_USER"] ?: error("DB_USER environment variable is required"),
        password = env["DB_PASSWORD"] ?: error("DB_PASSWORD environment variable is required"),
    )
