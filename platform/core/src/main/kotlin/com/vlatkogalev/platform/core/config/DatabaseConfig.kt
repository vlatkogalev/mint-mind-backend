package com.vlatkogalev.platform.core.config

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
)

fun loadDatabaseConfig(env: Map<String, String> = System.getenv()): DatabaseConfig =
    DatabaseConfig(
        url = env["DB_URL"] ?: "jdbc:h2:mem:template;DB_CLOSE_DELAY=-1;NON_KEYWORDS=YEAR",
        user = env["DB_USER"] ?: "sa",
        password = env["DB_PASSWORD"] ?: "",
    )
