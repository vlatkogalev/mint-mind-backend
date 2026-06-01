package com.vlatkogalev.platform.core.config

import java.util.Base64

data class EbayConfig(
    val oauthEndpoint: String,
    val oauthScope: String,
    val clientId: String,
    val clientSecret: String,
    val marketplaceId: String,
    val environment: EbayEnvironment,
    val maxResultsPerQuery: Int,
    val feedPagesToFetch: Int,
    val feedRefreshIntervalSeconds: Long,
) {
    val authHeaderValue: String
        get() = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())
}

enum class EbayEnvironment {
    SANDBOX, PRODUCTION;

    val browseApiBaseUrl: String
        get() = when (this) {
            SANDBOX -> "https://api.sandbox.ebay.com/buy/browse/v1"
            PRODUCTION -> "https://api.ebay.com/buy/browse/v1"
        }

    val oauthTokenUrl: String
        get() = when (this) {
            SANDBOX -> "https://api.sandbox.ebay.com/identity/v1/oauth2/token"
            PRODUCTION -> "https://api.ebay.com/identity/v1/oauth2/token"
        }
}

fun loadEbayConfig(env: Map<String, String> = System.getenv()): EbayConfig =
    run {
        val environment = if (env["EBAY_ENVIRONMENT"]?.uppercase() == "PRODUCTION")
            EbayEnvironment.PRODUCTION else EbayEnvironment.SANDBOX

        EbayConfig(
            oauthEndpoint = env["EBAY_OAUTH_ENDPOINT"] ?: environment.oauthTokenUrl,
            oauthScope = env["EBAY_OAUTH_SCOPE"]
                ?: "https://api.ebay.com/oauth/api_scope/buy.marketplace.search",
            clientId = env["EBAY_CLIENT_ID"] ?: error("EBAY_CLIENT_ID env var is required"),
            clientSecret = env["EBAY_CLIENT_SECRET"] ?: error("EBAY_CLIENT_SECRET env var is required"),
            marketplaceId = env["EBAY_MARKETPLACE_ID"] ?: "EBAY_US",
            environment = environment,
            maxResultsPerQuery = env["EBAY_MAX_RESULTS"]?.toIntOrNull() ?: 10,
            feedPagesToFetch = env["EBAY_FEED_PAGES"]?.toIntOrNull() ?: 5,
            feedRefreshIntervalSeconds = env["EBAY_FEED_REFRESH_INTERVAL_SECONDS"]?.toLongOrNull() ?: 600,
        )
    }
