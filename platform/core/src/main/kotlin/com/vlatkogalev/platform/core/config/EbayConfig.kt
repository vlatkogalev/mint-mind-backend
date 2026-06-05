package com.vlatkogalev.platform.core.config

import java.util.Base64

enum class EbayEnvironment(val browseApiBaseUrl: String) {
    SANDBOX("https://api.sandbox.ebay.com/buy/browse/v1"),
    PRODUCTION("https://api.ebay.com/buy/browse/v1"),
}

data class EbayConfig(
    val clientId: String,
    val clientSecret: String,
    val oauthEndpoint: String,
    val oauthScope: String,
    val marketplaceId: String,
    val environment: EbayEnvironment,
    val maxResults: Int,
    val feedPages: Int,
    val feedRefreshIntervalSeconds: Int,
) {
    val authHeaderValue: String by lazy {
        Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())
    }
}

fun loadEbayConfig(env: Map<String, String> = System.getenv()): EbayConfig =
    EbayConfig(
        clientId = env["EBAY_CLIENT_ID"] ?: "",
        clientSecret = env["EBAY_CLIENT_SECRET"] ?: "",
        oauthEndpoint = env["EBAY_OAUTH_ENDPOINT"] ?: "https://api.ebay.com/identity/v1/oauth2/token",
        oauthScope = env["EBAY_OAUTH_SCOPE"] ?: "https://api.ebay.com/oauth/api_scope",
        marketplaceId = env["EBAY_MARKETPLACE_ID"] ?: "EBAY_US",
        environment = EbayEnvironment.valueOf(env["EBAY_ENVIRONMENT"] ?: "SANDBOX"),
        maxResults = env["EBAY_MAX_RESULTS"]?.toIntOrNull() ?: 10,
        feedPages = env["EBAY_FEED_PAGES"]?.toIntOrNull() ?: 5,
        feedRefreshIntervalSeconds = env["EBAY_FEED_REFRESH_INTERVAL_SECONDS"]?.toIntOrNull() ?: 600,
    )
