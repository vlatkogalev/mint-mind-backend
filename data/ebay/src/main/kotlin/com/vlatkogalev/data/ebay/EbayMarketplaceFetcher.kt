package com.vlatkogalev.data.ebay

import com.vlatkogalev.platform.core.StructuredLogger
import com.vlatkogalev.platform.core.config.EbayConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class EbayMarketplaceFetcher(
    private val tokenProvider: EbayTokenProvider,
    private val config: EbayConfig,
) {
    private val logger = StructuredLogger("EbayMarketplaceFetcher")
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun fetchListings(pagesToFetch: Int = 5): List<EbayItemSummary> {
        val token = tokenProvider.getAccessToken()
        val baseUrl = config.environment.browseApiBaseUrl
        val exclusions = "-note -banknote -bill -currency -lot -set -roll -group -collection -box -boxes -album -coincard -collectible"

        return coroutineScope {
            val deferreds = (0 until pagesToFetch).map { page ->
                async {
                    try {
                        val response: EbaySearchResponse = client
                            .get("$baseUrl/item_summary/search") {
                                header("Authorization", "Bearer $token")
                                header("X-EBAY-C-MARKETPLACE-ID", config.marketplaceId)
                                parameter("q", "coin $exclusions")
                                parameter("category_ids", "11116")
                                parameter("filter", "buyingOptions:{FIXED_PRICE|AUCTION},price:[50..],priceCurrency:USD")
                                parameter("sort", "newlyListed")
                                parameter("limit", 200)
                                parameter("offset", page * 200)
                            }.body()
                        response.itemSummaries ?: emptyList()
                    } catch (e: Exception) {
                        logger.error("Failed to fetch eBay listings page", mapOf("page" to page), e)
                        emptyList()
                    }
                }
            }

            val allItems = mutableListOf<EbayItemSummary>()
            for ((index, deferred) in deferreds.withIndex()) {
                val items = deferred.await()
                if (items.isEmpty()) {
                    deferreds.drop(index + 1).forEach { it.cancel() }
                    break
                }
                allItems.addAll(items)
                if (items.size < 200) {
                    deferreds.drop(index + 1).forEach { it.cancel() }
                    break
                }
            }
            allItems
        }
    }
}

@Serializable
data class EbaySearchResponse(
    val itemSummaries: List<EbayItemSummary>? = null,
)

@Serializable
data class EbayItemSummary(
    val itemId: String? = null,
    val title: String? = null,
    val price: EbayPrice? = null,
    val condition: String? = null,
    val itemWebUrl: String? = null,
    val image: EbayImage? = null,
    val thumbnailImages: List<EbayImage>? = null,
    val buyingOptions: List<String>? = null,
    val itemEndDate: String? = null,
)

@Serializable
data class EbayPrice(
    val value: String? = null,
    val currency: String? = null,
)

@Serializable
data class EbayImage(
    val imageUrl: String? = null,
)
