package com.vlatkogalev.platform.core.config

data class EmailConfig(
    val resendApiKey: String,
    val fromAddress: String,
    val appBaseUrl: String,
)

fun loadEmailConfig(env: Map<String, String> = System.getenv()): EmailConfig =
    EmailConfig(
        resendApiKey = envValue(env, "RESEND_API_KEY") ?: "test-resend-api-key", // error("RESEND_API_KEY env var is required"),
        fromAddress = envValue(env, "EMAIL_FROM") ?: "noreply@mintmind.app",
        appBaseUrl = envValue(env, "APP_BASE_URL") ?: "http://localhost:8080",
    )

private fun envValue(env: Map<String, String>, key: String): String? =
    env[key] ?: System.getProperty(key)
