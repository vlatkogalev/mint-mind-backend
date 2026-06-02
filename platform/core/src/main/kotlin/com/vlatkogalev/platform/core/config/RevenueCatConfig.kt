package com.vlatkogalev.platform.core.config

data class RevenueCatConfig(
    val webhookSecret: String,
)

fun loadRevenueCatConfig(env: Map<String, String> = System.getenv()): RevenueCatConfig =
    RevenueCatConfig(
        webhookSecret = envValue(env, "REVENUECAT_WEBHOOK_SECRET") ?: "",
    )
