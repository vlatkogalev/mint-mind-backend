package com.vlatkogalev.platform.core.config

data class EbayConfig(
    val clientId: String,
    val clientSecret: String,
    val marketplaceId: String,
    val environment: EbayEnvironment,
    val maxResultsPerQuery: Int,
)

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
    EbayConfig(
        clientId = env["EBAY_CLIENT_ID"] ?: "",
        clientSecret = env["EBAY_CLIENT_SECRET"] ?: "",
        marketplaceId = env["EBAY_MARKETPLACE_ID"] ?: "EBAY_US",
        environment = if (env["EBAY_ENVIRONMENT"]?.uppercase() == "PRODUCTION")
            EbayEnvironment.PRODUCTION else EbayEnvironment.SANDBOX,
        maxResultsPerQuery = env["EBAY_MAX_RESULTS"]?.toIntOrNull() ?: 10,
    )