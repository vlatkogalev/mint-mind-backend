package com.vlatkogalev.platform.core.config

data class NumistaConfig(
    val apiKey: String,
    val baseUrl: String,
) {
    val enabled: Boolean get() = apiKey.isNotBlank()
}

fun loadNumistaConfig(env: Map<String, String> = System.getenv()): NumistaConfig =
    NumistaConfig(
        apiKey = env["NUMISTA_API_KEY"] ?: "",
        baseUrl = env["NUMISTA_BASE_URL"] ?: "https://api.numista.com",
    )
