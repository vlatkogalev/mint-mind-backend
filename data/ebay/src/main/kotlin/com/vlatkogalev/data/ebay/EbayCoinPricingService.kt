package com.vlatkogalev.data.ebay

import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.pricing.model.ActiveListing
import com.vlatkogalev.domain.pricing.model.CoinPricingResult
import com.vlatkogalev.domain.pricing.model.PriceRange
import com.vlatkogalev.domain.pricing.service.CoinPricingService
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.config.EbayConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

class EbayCoinPricingService(
    private val config: EbayConfig,
    private val tokenProvider: EbayTokenProvider,
) : CoinPricingService {

    private val log = LoggerFactory.getLogger(EbayCoinPricingService::class.java)
    private val http = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val excludedTerms = "-lot -set -roll -collection -pair -group -album -box -boxes -coincard -collectible -note -banknote -bill -currency"

    override suspend fun getPricing(coin: Coin, minResults: Int): Result<CoinPricingResult> =
        try {
            val token = withContext(Dispatchers.IO) { tokenProvider.getAccessToken() }
            val narrowQuery = buildQuery(coin, includeGrade = true)
            val broadQuery = buildQuery(coin, includeGrade = false)

            val (query, listings) = coroutineScope {
                val narrowDeferred = async(Dispatchers.IO) { fetchListings(narrowQuery, token) }

                if (narrowQuery == broadQuery) {
                    narrowQuery to narrowDeferred.await()
                } else {
                    val broadDeferred = async(Dispatchers.IO) { fetchListings(broadQuery, token) }
                    val narrow = narrowDeferred.await()
                    if (narrow.size >= minResults) {
                        broadDeferred.cancel()
                        narrowQuery to narrow
                    } else {
                        log.debug(
                            "EbayCoinPricingService: narrow query '{}' returned {} results, retrying broad",
                            narrowQuery, narrow.size,
                        )
                        broadQuery to broadDeferred.await()
                    }
                }
            }

            Result.Success(
                CoinPricingResult(
                    query = query,
                    listings = listings,
                    priceRange = computePriceRange(listings),
                    source = "eBay",
                    fetchedAt = Instant.now(),
                ),
            )
        } catch (ex: Exception) {
            log.error("EbayCoinPricingService: failed to fetch pricing", ex)
            Result.Failure(ex.message ?: "Failed to fetch eBay pricing", ex)
        }

    internal fun buildQuery(coin: Coin, includeGrade: Boolean): String {
        val r = coin.recognitionResult
        val krause = coin.catalogueNumbers
            .firstOrNull { it.catalogueName.contains("krause", ignoreCase = true) }
            ?.number

        return buildString {
            if (krause != null) append("$krause ")
            r.countryOrIssuer?.let { append("$it ") }
            r.year?.let { append("$it ") }
            r.denomination?.let { append("$it ") }
            r.seriesName?.let { append("$it ") }
            if (includeGrade) {
                val grade = r.estimatedGradeValue ?: r.estimatedGrade
                grade?.let { append("$it ") }
            }
        }.trim()
    }

    private fun fetchListings(query: String, token: String): List<ActiveListing> {
        if (query.isBlank()) return emptyList()

        val fullQuery = "$query $excludedTerms"
        val encodedQuery = URLEncoder.encode(fullQuery, Charsets.UTF_8)

        val url = "${config.environment.browseApiBaseUrl}/item_summary/search" +
                "?q=$encodedQuery" +
                "&category_ids=11116" +
                "&filter=buyingOptions:%7BFIXED_PRICE%7CAUCTION%7D" +
                "&limit=${config.maxResultsPerQuery}" +
                "&sort=newlyListed"

        log.debug("EbayCoinPricingService: GET {}", url)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer $token")
            .header("X-EBAY-C-MARKETPLACE-ID", config.marketplaceId)
            .header("Content-Type", "application/json")
            .GET()
            .build()

        val response = http.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() !in 200..299) {
            log.error(
                "EbayCoinPricingService: Browse API returned HTTP {}: {}",
                response.statusCode(), response.body().take(500),
            )
            check(false) { "eBay Browse API returned HTTP ${response.statusCode()}" }
        }

        val searchResponse = json.decodeFromString<EbaySearchResponse>(response.body())
        return searchResponse.itemSummaries.orEmpty().map { it.toActiveListing() }
    }

    private fun computePriceRange(listings: List<ActiveListing>): PriceRange? {
        if (listings.isEmpty()) return null
        val prices = listings.map { it.currentPrice }.sorted()
        return PriceRange(
            low = prices.first(),
            high = prices.last(),
            median = prices[prices.size / 2],
            mean = prices.average(),
            sampleSize = prices.size,
        )
    }

    @Serializable
    private data class EbaySearchResponse(
        val itemSummaries: List<EbayItemSummary>? = null,
    )

    @Serializable
    private data class EbayItemSummary(
        val title: String,
        val price: EbayPrice? = null,
        val condition: String? = null,
        val itemWebUrl: String = "",
        val image: EbayImage? = null,
        val itemEndDate: String? = null,
        val buyingOptions: List<String> = emptyList(),
    ) {
        fun toActiveListing() = ActiveListing(
            title = title,
            currentPrice = price?.value?.toDoubleOrNull() ?: 0.0,
            currency = price?.currency ?: "USD",
            condition = condition,
            listingUrl = itemWebUrl,
            imageUrl = image?.imageUrl,
            listingEndDate = itemEndDate?.let { runCatching { Instant.parse(it) }.getOrNull() },
            buyingOptions = buyingOptions,
        )
    }

    @Serializable
    private data class EbayPrice(
        val value: String,
        val currency: String,
    )

    @Serializable
    private data class EbayImage(
        val imageUrl: String,
    )
}
