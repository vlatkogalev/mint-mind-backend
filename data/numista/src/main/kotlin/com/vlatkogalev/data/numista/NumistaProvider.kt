package com.vlatkogalev.data.numista

import com.vlatkogalev.data.numista.dto.NumistaTypeDetail
import com.vlatkogalev.data.numista.dto.NumistaTypesSearchResponse
import com.vlatkogalev.domain.coin.model.CoinCatalogCandidate
import com.vlatkogalev.domain.coin.model.CoinFingerprint
import com.vlatkogalev.domain.coin.model.ExternalCoinReference
import com.vlatkogalev.domain.coin.service.CoinCatalogProvider
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.config.NumistaConfig
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.UUID

class NumistaProvider(
    private val config: NumistaConfig,
) : CoinCatalogProvider {

    override val providerName: String = "numista"

    private val log = LoggerFactory.getLogger(NumistaProvider::class.java)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    override fun findCandidates(fingerprint: CoinFingerprint): Result<List<CoinCatalogCandidate>> {
        if (!config.enabled) return Result.Success(emptyList())

        val query = buildQuery(fingerprint)
        if (query.isBlank()) return Result.Success(emptyList())

        return try {
            val searchBody = get("${config.apiBaseUrl}/v3/types?category=coin&q=${urlEncode(query)}&count=10")
            val searchResponse = json.decodeFromString<NumistaTypesSearchResponse>(searchBody)
            val candidates = searchResponse.types.map { type ->
                val detail = fetchDetail(type.id)
                CoinCatalogCandidate(
                    externalReference = ExternalCoinReference(
                        id = UUID.randomUUID(),
                        catalogCoinId = UNASSIGNED_CATALOG_COIN_ID,
                        provider = providerName,
                        externalId = type.id.toString(),
                        externalUrl = detail?.url ?: "https://en.numista.com/catalogue/pieces${type.id}.html",
                        lastSyncedAt = null,
                        syncStatus = null,
                        syncError = null,
                        createdAt = Instant.now(),
                    ),
                    title = type.title,
                    countryOrIssuer = type.country?.name ?: type.issuer?.name,
                    denomination = type.type,
                    yearStart = type.yearStart,
                    yearEnd = type.yearEnd,
                )
            }
            Result.Success(candidates)
        } catch (ex: Exception) {
            log.warn("Numista lookup failed", ex)
            Result.Failure(ex.message ?: "Failed to fetch Numista candidates", ex)
        }
    }

    private fun fetchDetail(typeId: Long): NumistaTypeDetail? =
        try {
            val body = get("${config.apiBaseUrl}/v3/types/$typeId")
            json.decodeFromString<NumistaTypeDetail>(body)
        } catch (ex: Exception) {
            log.debug("Numista detail fetch failed for type {}", typeId, ex)
            null
        }

    private fun get(url: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(REQUEST_TIMEOUT)
            .header("Numista-API-Key", config.apiKey)
            .header("Accept", "application/json")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) {
            "Numista API error: ${response.statusCode()} ${response.body().take(400)}"
        }
        return response.body()
    }

    private fun buildQuery(fingerprint: CoinFingerprint): String =
        listOf(
            fingerprint.countryOrIssuer,
            fingerprint.denomination,
            fingerprint.title,
            fingerprint.seriesName,
            fingerprint.year?.toString(),
            fingerprint.mintMark,
        )
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
            .joinToString(" ")

    private fun urlEncode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8)

    private companion object {
        val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(3)
        val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(4)
        val UNASSIGNED_CATALOG_COIN_ID: UUID = UUID(0L, 0L)
    }
}
