package com.vlatkogalev.platform.core.config

data class EmailConfig(
    val resendApiKey: String,
    val fromAddress: String,
    val appBaseUrl: String,
    val skipVerification: Boolean,
)

fun loadEmailConfig(env: Map<String, String> = System.getenv()): EmailConfig {
    val skipVerification = envValue(env, "APP_ENV") == "local"
    return EmailConfig(
        resendApiKey = if (skipVerification) "" else envValue(env, "RESEND_API_KEY")
            ?: error("RESEND_API_KEY env var is required"),
        fromAddress = envValue(env, "EMAIL_FROM") ?: "noreply@mintmind.app",
        appBaseUrl = envValue(env, "APP_BASE_URL") ?: "http://localhost:8080",
        skipVerification = skipVerification,
    )
}

private fun envValue(env: Map<String, String>, key: String): String? =
    env[key] ?: System.getProperty(key)
