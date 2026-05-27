package com.vlatkogalev.data.ebay

import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.pricing.model.ActiveListing
import com.vlatkogalev.domain.pricing.model.CoinPricingResult
import com.vlatkogalev.domain.pricing.model.PriceRange
import com.vlatkogalev.domain.pricing.service.CoinPricingService
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.config.EbayConfig
import com.vlatkogalev.platform.core.config.loadEbayConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference

class EbayCoinPricingService(
    private val config: EbayConfig = loadEbayConfig(),
) : CoinPricingService {

    private val log = LoggerFactory.getLogger(EbayCoinPricingService::class.java)
    private val http = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val cachedToken = AtomicReference<CachedToken?>(null)

    override fun getPricing(coin: Coin, minResults: Int): Result<CoinPricingResult> =
        try {
            if (config.clientId.isBlank() || config.clientSecret.isBlank()) {
                log.warn("EbayCoinPricingService: EBAY_CLIENT_ID or EBAY_CLIENT_SECRET not configured")
                return Result.Success(emptyResult(buildQuery(coin, includeGrade = false)))
            }

            val token = getAccessToken()

            val narrowQuery = buildQuery(coin, includeGrade = true)
            val narrowListings = fetchListings(narrowQuery, token)

            val (query, listings) = if (narrowListings.size >= minResults) {
                narrowQuery to narrowListings
            } else {
                log.debug(
                    "EbayCoinPricingService: narrow query '{}' returned {} results, retrying broad",
                    narrowQuery, narrowListings.size,
                )
                val broadQuery = buildQuery(coin, includeGrade = false)
                broadQuery to fetchListings(broadQuery, token)
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

    private fun getAccessToken(): String {
        val existing = cachedToken.get()
        if (existing != null && !existing.isExpired()) return existing.token

        val fresh = fetchNewToken()
        cachedToken.set(fresh)
        return fresh.token
    }

    private fun fetchNewToken(): CachedToken {
        val credentials = Base64.getEncoder()
            .encodeToString("${config.clientId}:${config.clientSecret}".toByteArray())

        val request = HttpRequest.newBuilder()
            .uri(URI.create(config.environment.oauthTokenUrl))
            .header("Authorization", "Basic $credentials")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "grant_type=client_credentials" +
                            "&scope=${URLEncoder.encode("https://api.ebay.com/oauth/api_scope", Charsets.UTF_8)}",
                ),
            )
            .build()

        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) {
            "eBay OAuth token request failed with HTTP ${response.statusCode()}: ${response.body()}"
        }

        val tokenResponse = json.decodeFromString<EbayTokenResponse>(response.body())
        val expiresAt = Instant.now().plusSeconds(tokenResponse.expiresIn - 60)

        log.debug("EbayCoinPricingService: fetched new OAuth token, expires at {}", expiresAt)
        return CachedToken(token = tokenResponse.accessToken, expiresAt = expiresAt)
    }

    private fun fetchListings(query: String, token: String): List<ActiveListing> {
        if (query.isBlank()) return emptyList()

        val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8)
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

    private fun emptyResult(query: String) = CoinPricingResult(
        query = query,
        listings = emptyList(),
        priceRange = null,
        source = "eBay",
        fetchedAt = Instant.now(),
    )

    private data class CachedToken(val token: String, val expiresAt: Instant) {
        fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
    }

    @Serializable
    private data class EbayTokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("expires_in") val expiresIn: Long,
    )

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