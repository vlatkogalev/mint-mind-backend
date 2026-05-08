package com.vlatkogalev.platform.core.config

data class AppConfig(
    val port: Int,
    val environment: String,
)

fun loadAppConfig(env: Map<String, String> = System.getenv()): AppConfig =
    AppConfig(
        port = env["PORT"]?.toIntOrNull() ?: 8080,
        environment = env["APP_ENV"] ?: "local",
    )
