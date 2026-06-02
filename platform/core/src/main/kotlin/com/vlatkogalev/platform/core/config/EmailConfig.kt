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
        resendApiKey = if (skipVerification) "" else requiredEnv(env, "RESEND_API_KEY"),
        fromAddress = envValue(env, "EMAIL_FROM") ?: "noreply@mintmind.app",
        appBaseUrl = envValue(env, "APP_BASE_URL") ?: "http://localhost:8080",
        skipVerification = skipVerification,
    )
}
