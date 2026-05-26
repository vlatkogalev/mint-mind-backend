package com.vlatkogalev.data.ebay

import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.pricing.model.CoinPricingResult
import com.vlatkogalev.domain.pricing.model.PriceRange
import com.vlatkogalev.domain.pricing.model.SoldListing
import com.vlatkogalev.domain.pricing.service.CoinPricingService
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.config.EbayConfig
import com.vlatkogalev.platform.core.config.loadEbayConfig
import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory

class EbayCoinPricingService(
    private val config: EbayConfig = loadEbayConfig(),
) : CoinPricingService {

    private val log = LoggerFactory.getLogger(EbayCoinPricingService::class.java)
    private val http = HttpClient.newHttpClient()
    private val docFactory = DocumentBuilderFactory.newInstance()

    override fun getPricing(coin: Coin, minResults: Int): Result<CoinPricingResult> =
        try {
            val narrowQuery = buildQuery(coin, includeGrade = true)
            val narrowResults = fetchSoldListings(narrowQuery)

            val (query, listings) = if (narrowResults.size >= minResults) {
                narrowQuery to narrowResults
            } else {
                val broadQuery = buildQuery(coin, includeGrade = false)
                log.debug(
                    "EbayCoinPricingService: narrow query '{}' returned {} results, retrying broad",
                    narrowQuery, narrowResults.size,
                )
                broadQuery to fetchSoldListings(broadQuery)
            }

            Result.Success(
                CoinPricingResult(
                    query = query,
                    recentSales = listings,
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
            if (krause != null) {
                append(krause)
                append(" ")
            }
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

    private fun fetchSoldListings(query: String): List<SoldListing> {
        if (config.appId.isBlank()) {
            log.warn("EbayCoinPricingService: EBAY_APP_ID not configured, returning empty results")
            return emptyList()
        }

        val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8)
        val url = buildUrl(encodedQuery)

        log.debug("EbayCoinPricingService: GET {}", url)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/xml")
            .GET()
            .build()

        val response = http.send(request, HttpResponse.BodyHandlers.ofString())

        log.debug(
            "EbayCoinPricingService: response status={} body={}",
            response.statusCode(),
            response.body().take(500)
        )

        check(response.statusCode() in 200..299) {
            "eBay API returned HTTP ${response.statusCode()}"
        }

        return parseListings(response.body())
    }

    private fun buildUrl(encodedQuery: String): String {
        val base = config.environment.findingApiBaseUrl
        return base +
                "?OPERATION-NAME=findCompletedItems" +
                "&SERVICE-VERSION=1.13.0" +
                "&SECURITY-APPNAME=${config.appId}" +
                "&RESPONSE-DATA-FORMAT=XML" +
                "&keywords=$encodedQuery" +
                "&itemFilter(0).name=SoldItemsOnly" +
                "&itemFilter(0).value=true" +
                "&categoryId=11116" +
                "&paginationInput.entriesPerPage=${config.maxResultsPerQuery}" +
                "&sortOrder=EndTimeSoonest"
    }

    private fun parseListings(xml: String): List<SoldListing> {
        val doc = docFactory.newDocumentBuilder()
            .parse(xml.byteInputStream())

        doc.documentElement.normalize()

        val items = doc.getElementsByTagName("item")
        return (0 until items.length).mapNotNull { i ->
            runCatching { parseItem(items.item(i) as Element) }
                .onFailure { log.warn("EbayCoinPricingService: skipping item due to parse error", it) }
                .getOrNull()
        }
    }

    private fun parseItem(item: Element): SoldListing {
        val title = item.firstText("title")
        val price = item.firstText("currentPrice").toDouble()
        val currency = item.getElementsByTagName("currentPrice")
            .let { it.item(0) as? Element }
            ?.getAttribute("currencyId") ?: "USD"
        val endTime = Instant.parse(item.firstText("endTime"))
        val condition = runCatching { item.firstText("conditionDisplayName") }.getOrNull()
        val url = item.firstText("viewItemURL")
        val imageUrl = runCatching { item.firstText("galleryURL") }.getOrNull()

        return SoldListing(
            title = title,
            soldPrice = price,
            currency = currency,
            soldAt = endTime,
            condition = condition,
            listingUrl = url,
            imageUrl = imageUrl,
        )
    }

    private fun Element.firstText(tag: String): String =
        (getElementsByTagName(tag).item(0) as Element).textContent.trim()

    private fun computePriceRange(listings: List<SoldListing>): PriceRange? {
        if (listings.isEmpty()) return null
        val prices = listings.map { it.soldPrice }.sorted()
        return PriceRange(
            low = prices.first(),
            high = prices.last(),
            median = prices[prices.size / 2],
            mean = prices.average(),
            sampleSize = prices.size,
        )
    }
}