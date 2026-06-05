package com.vlatkogalev.data.ebay

import com.vlatkogalev.domain.coin.model.CatalogueNumber
import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.pricing.model.ActiveListing
import com.vlatkogalev.domain.pricing.model.CoinPricingResult
import com.vlatkogalev.domain.pricing.model.PriceRange
import com.vlatkogalev.domain.pricing.service.CoinPricingService
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.config.EbayConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

class EbayCoinPricingService(
    private val tokenProvider: EbayTokenProvider,
    private val config: EbayConfig,
) : CoinPricingService {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val exclusions = "-lot -set -roll -collection -pair -group -album -box -boxes -coincard -collectible -note -banknote -bill -currency"

    override suspend fun getPricing(coin: Coin, minResults: Int): Result<CoinPricingResult> =
        try {
            val narrowQuery = buildQuery(coin, includeGrade = true)
            val broadQuery = buildQuery(coin, includeGrade = false)

            if (narrowQuery.isBlank() && broadQuery.isBlank()) {
                return Result.Success(
                    CoinPricingResult(
                        query = "",
                        listings = emptyList(),
                        priceRange = null,
                        source = "eBay",
                        fetchedAt = Instant.now(),
                    )
                )
            }

            val result = coroutineScope {
                val narrowDeferred = if (narrowQuery.isNotBlank()) {
                    async { fetchListings(narrowQuery) }
                } else null

                val broadDeferred = if (broadQuery != narrowQuery && broadQuery.isNotBlank()) {
                    async { fetchListings(broadQuery) }
                } else null

                val narrowResults = narrowDeferred?.await() ?: emptyList()

                if (narrowResults.size >= minResults || broadDeferred == null) {
                    narrowResults
                } else {
                    broadDeferred.await()
                }
            }

            val priceRange = if (result.isNotEmpty()) {
                val prices = result.map { it.currentPrice }.sorted()
                PriceRange(
                    low = prices.first(),
                    high = prices.last(),
                    median = prices[prices.size / 2],
                    mean = prices.sum() / prices.size,
                    sampleSize = prices.size,
                )
            } else null

            Result.Success(
                CoinPricingResult(
                    query = narrowQuery.ifBlank { broadQuery },
                    listings = result,
                    priceRange = priceRange,
                    source = "eBay",
                    fetchedAt = Instant.now(),
                )
            )
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to get pricing", ex)
        }

    private suspend fun fetchListings(query: String): List<ActiveListing> {
        val token = tokenProvider.getAccessToken()
        val baseUrl = config.environment.browseApiBaseUrl

        val fullQuery = if (query.isNotBlank()) "$query $exclusions" else ""

        val response: EbaySearchResponse = client
            .get("$baseUrl/item_summary/search") {
                header("Authorization", "Bearer $token")
                header("X-EBAY-C-MARKETPLACE-ID", config.marketplaceId)
                parameter("q", fullQuery)
                parameter("category_ids", "11116")
                parameter("filter", "buyingOptions:{FIXED_PRICE|AUCTION}")
                parameter("limit", config.maxResults)
                parameter("sort", "newlyListed")
            }.body()

        return response.itemSummaries?.mapNotNull { it.toActiveListing() } ?: emptyList()
    }

    fun buildQuery(coin: Coin, includeGrade: Boolean = true): String {
        val rr = coin.recognitionResult
        val parts = mutableListOf<String>()

        val krauseNumber = coin.catalogueNumbers.firstOrNull {
            it.catalogueName.contains("krause", ignoreCase = true) || it.catalogueName.contains("km#", ignoreCase = true)
        }?.number
        if (!krauseNumber.isNullOrBlank()) parts.add(krauseNumber)

        rr.countryOrIssuer?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        rr.year?.let { parts.add(it.toString()) }
        rr.denomination?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        rr.seriesName?.takeIf { it.isNotBlank() }?.let { parts.add(it) }

        if (includeGrade) {
            rr.estimatedGradeValue?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
                ?: rr.estimatedGrade?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        }

        return parts.joinToString(" ")
    }

    private fun EbayItemSummary.toActiveListing(): ActiveListing? {
        val title = title ?: return null
        return ActiveListing(
            title = title,
            currentPrice = price?.value?.toDoubleOrNull() ?: 0.0,
            currency = price?.currency ?: "USD",
            condition = condition,
            listingUrl = itemWebUrl ?: "",
            imageUrl = image?.imageUrl,
            listingEndDate = itemEndDate?.let { runCatching { Instant.parse(it) }.getOrNull() },
            buyingOptions = buyingOptions ?: emptyList(),
        )
    }
}
