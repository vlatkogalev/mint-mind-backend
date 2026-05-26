package com.vlatkogalev.platform.core.config

data class EbayConfig(
    val appId: String,
    val environment: EbayEnvironment,
    val maxResultsPerQuery: Int,
    val soldDaysLookback: Int,
)

enum class EbayEnvironment {
    SANDBOX, PRODUCTION;

    val findingApiBaseUrl: String
        get() = when (this) {
            SANDBOX -> "https://svcs.sandbox.ebay.com/services/search/FindingService/v1"
            PRODUCTION -> "https://svcs.ebay.com/services/search/FindingService/v1"
        }
}

fun loadEbayConfig(env: Map<String, String> = System.getenv()): EbayConfig =
    EbayConfig(
        appId = env["EBAY_APP_ID"] ?: "",
        environment = if (env["EBAY_ENVIRONMENT"]?.uppercase() == "PRODUCTION")
            EbayEnvironment.PRODUCTION else EbayEnvironment.SANDBOX,
        maxResultsPerQuery = env["EBAY_MAX_RESULTS"]?.toIntOrNull() ?: 10,
        soldDaysLookback = env["EBAY_SOLD_DAYS_LOOKBACK"]?.toIntOrNull() ?: 90,
    )