package com.vlatkogalev.data.ebay

import com.vlatkogalev.domain.marketplace.model.MarketplaceListing
import com.vlatkogalev.platform.core.config.EbayConfig
import kotlinx.coroutines.Dispatchers
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
import java.util.UUID

class EbayMarketplaceFetcher(
    private val config: EbayConfig,
    private val tokenProvider: EbayTokenProvider,
) {
    private val log = LoggerFactory.getLogger(EbayMarketplaceFetcher::class.java)
    private val httpClient = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchListings(pagesToFetch: Int = 5): List<MarketplaceListing> =
        withContext(Dispatchers.IO) { blockingFetchListings(pagesToFetch) }

    private fun blockingFetchListings(pagesToFetch: Int): List<MarketplaceListing> {
        if (pagesToFetch <= 0) return emptyList()

        val allListings = mutableListOf<MarketplaceListing>()
        for (pageNum in 0 until pagesToFetch) {
            val offset = pageNum * PAGE_SIZE
            log.info("Fetching eBay listings page {} (offset={})", pageNum + 1, offset)

            val page = fetchPage(offset, PAGE_SIZE)
            if (page.isEmpty()) break

            allListings.addAll(page)
            if (page.size < PAGE_SIZE) break
        }

        log.info("Fetched {} total listings from eBay", allListings.size)
        return allListings
    }

    private fun fetchPage(offset: Int, limit: Int): List<MarketplaceListing> {
        val token = tokenProvider.getAccessToken()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(buildBrowseApiUrl(offset, limit)))
            .header("Authorization", "Bearer $token")
            .header("X-EBAY-C-MARKETPLACE-ID", config.marketplaceId)
            .header("Accept", "application/json")
            .GET()
            .build()

        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            check(response.statusCode() in 200..299) {
                "eBay Browse API error: ${response.statusCode()} ${response.body()}"
            }
            parseListings(response.body())
        } catch (ex: Exception) {
            log.error("Failed to fetch eBay listings page at offset {}", offset, ex)
            emptyList()
        }
    }

    private fun buildBrowseApiUrl(offset: Int, limit: Int): String {
        val q = "coin -note -banknote -bill -currency -lot -set -roll -group " +
            "-collection -box -boxes -album -coincard -collectible"
        val categoryIds = "11116"
        val aspectFilter = "Certification:{PCGS|NGC|ANACS}"
        val priceFilter = "50.."

        return buildString {
            append(config.environment.browseApiBaseUrl).append("/item_summary/search?")
            append("q=").append(urlEncode(q))
            append("&category_ids=").append(categoryIds)
            append("&filter=price:[").append(priceFilter).append("],priceCurrency:USD")
            append("&aspect_filter=").append(urlEncode("categoryId:$categoryIds,$aspectFilter"))
            append("&sort=newlyListed")
            append("&limit=").append(limit)
            append("&offset=").append(offset)
        }
    }

    private fun parseListings(jsonResponse: String): List<MarketplaceListing> {
        val response = runCatching { json.decodeFromString<EbaySearchResponse>(jsonResponse) }.getOrElse {
            log.warn("Failed to parse eBay listings response", it)
            return emptyList()
        }

        val now = Instant.now()
        return response.itemSummaries.orEmpty().mapNotNull { item ->
            runCatching {
                val itemId = item.itemId ?: return@runCatching null
                val title = item.title ?: return@runCatching null
                MarketplaceListing(
                    id = UUID.randomUUID(),
                    ebayItemId = itemId,
                    title = title,
                    price = item.price?.value ?: "0",
                    currency = item.price?.currency ?: "USD",
                    condition = item.condition,
                    listingUrl = item.itemWebUrl ?: "https://www.ebay.com/itm/$itemId",
                    imageUrl = item.image?.imageUrl,
                    buyingOptions = item.buyingOptions.orEmpty(),
                    expiresAt = item.itemEndDate?.let { Instant.parse(it) },
                    lastSeenAt = now,
                )
            }.getOrNull()
        }
    }

    private fun urlEncode(str: String): String =
        URLEncoder.encode(str, Charsets.UTF_8)

    @Serializable
    private data class EbaySearchResponse(
        val itemSummaries: List<EbayItemSummary>? = null,
    )

    @Serializable
    private data class EbayItemSummary(
        val itemId: String? = null,
        val title: String? = null,
        val price: EbayPrice? = null,
        val condition: String? = null,
        val itemWebUrl: String? = null,
        val image: EbayImage? = null,
        val itemEndDate: String? = null,
        val buyingOptions: List<String>? = null,
    )

    @Serializable
    private data class EbayPrice(
        val value: String? = null,
        val currency: String? = null,
    )

    @Serializable
    private data class EbayImage(
        val imageUrl: String? = null,
    )

    companion object {
        private const val PAGE_SIZE = 200
    }
}
