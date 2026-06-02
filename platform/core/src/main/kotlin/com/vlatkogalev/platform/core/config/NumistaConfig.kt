package com.vlatkogalev.platform.core.config

data class NumistaConfig(
    val apiBaseUrl: String,
    val apiKey: String,
    val enabled: Boolean,
)

fun loadNumistaConfig(env: Map<String, String> = System.getenv()): NumistaConfig {
    val apiKey = envValue(env, "NUMISTA_API_KEY").orEmpty().trim()
    return NumistaConfig(
        apiBaseUrl = envValue(env, "NUMISTA_API_BASE_URL") ?: "https://api.numista.com/api",
        apiKey = apiKey,
        enabled = apiKey.isNotBlank(),
    )
}
