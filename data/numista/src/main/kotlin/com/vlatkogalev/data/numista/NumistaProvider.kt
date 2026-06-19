package com.vlatkogalev.data.numista

import com.vlatkogalev.domain.coin.model.CoinCatalogCandidate
import com.vlatkogalev.domain.coin.model.CoinFingerprint
import com.vlatkogalev.domain.coin.model.CountryAliasMapping
import com.vlatkogalev.domain.coin.model.ExternalCoinReference
import com.vlatkogalev.domain.coin.service.CoinCatalogProvider
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.config.NumistaConfig
import com.vlatkogalev.platform.core.StructuredLogger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import java.util.UUID

class NumistaProvider(
    private val config: NumistaConfig,
) : CoinCatalogProvider {
    override val providerName: String = "Numista"

    private val logger = StructuredLogger("NumistaProvider")

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        engine {
            requestTimeout = 4000
        }
    }

    private var issuerCodeCache: Map<String, String>? = null

    private suspend fun ensureIssuerCache() {
        if (issuerCodeCache != null) return
        if (!config.enabled) {
            issuerCodeCache = emptyMap()
            return
        }
        try {
            val response: NumistaIssuersResponse = client
                .get("${config.baseUrl}/v3/issuers") {
                    header("Numista-API-Key", config.apiKey)
                }.body()
            issuerCodeCache = response.issuers
                ?.associate { it.name.lowercase() to it.code }
                ?: emptyMap()
            logger.info("numista loaded ${issuerCodeCache!!.size} issuer codes")
        } catch (e: Exception) {
            logger.error("numista issuer cache load failed — will retry on next scan", throwable = e)
        }
    }

    override suspend fun findCandidates(fingerprint: CoinFingerprint): Result<List<CoinCatalogCandidate>> =
        try {
            if (!config.enabled) {
                return Result.Success(emptyList())
            }

            ensureIssuerCache()

            val query = buildQuery(fingerprint)
            logger.info("numista query='$query'")
            if (query.isBlank()) return Result.Success(emptyList())

            val searchResponse: NumistaTypesSearchResponse = client
                .get("${config.baseUrl}/v3/types") {
                    parameter("category", "coin")
                    parameter("q", query)
                    parameter("count", 3)
                    header("Numista-API-Key", config.apiKey)

                    fingerprint.year?.let { parameter("year", it.toString()) }

                    fingerprint.diameterMm?.let { d ->
                        val lo = (d - 2.0).coerceAtLeast(0.0).toInt()
                        val hi = (d + 2.0).toInt()
                        parameter("size", "$lo-$hi")
                    }

                    fingerprint.weightGrams?.let { w ->
                        val lo = (w - 1.0).coerceAtLeast(0.0).toInt()
                        val hi = (w + 1.0).toInt()
                        parameter("weight", "$lo-$hi")
                    }

                    fingerprint.countryOrIssuer
                        ?.let { CountryAliasMapping.normalize(it) }
                        ?.let { normalized -> issuerCodeCache?.get(normalized) }
                        ?.let { code -> parameter("issuer", code) }
                }.body()

            val summaries = searchResponse.types ?: emptyList()
            logger.info("numista searchResults=${summaries.size}")

            val filteredSummaries = if (fingerprint.year != null) {
                val fpYear = fingerprint.year!!
                summaries.filter { summary ->
                    val min = summary.minYear
                    val max = summary.maxYear
                    when {
                        min != null && max != null -> fpYear in min..max
                        min != null -> fpYear >= min
                        max != null -> fpYear <= max
                        else -> true
                    }
                }.also { filtered ->
                    val skipped = summaries.size - filtered.size
                    if (skipped > 0) {
                        logger.info("numista pre-filter skipped $skipped/${summaries.size} summaries by year range")
                    }
                }
            } else {
                summaries
            }

            coroutineScope {
                filteredSummaries.map { summary ->
                    logger.info("numista fetching typeId=${summary.id}")
                    async {
                        try {
                            val detail: NumistaTypeDetail = client
                                .get("${config.baseUrl}/v3/types/${summary.id}") {
                                    header("Numista-API-Key", config.apiKey)
                                }.body()
                            detail.toCandidate(summary.id)
                        } catch (e: Exception) {
                            logger.error("numista detail fetch failed typeId=${summary.id} error=${e::class.simpleName} message=${e.message}")
                            null
                        }
                    }
                }.mapNotNull { it.await() }
            }.let { Result.Success(it) }
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Numista API error", ex)
        }

    private fun buildQuery(fingerprint: CoinFingerprint): String =
        listOfNotNull(
            fingerprint.countryOrIssuer,
            fingerprint.denomination,
            fingerprint.year?.toString(),
        ).joinToString(" ").trim()

    private fun NumistaTypeDetail.toCandidate(externalId: Int): CoinCatalogCandidate {
        val ref = ExternalCoinReference(
            id = UUID.randomUUID(),
            catalogCoinId = UUID.randomUUID(),
            provider = providerName,
            externalId = externalId.toString(),
            externalUrl = url,
            lastSyncedAt = java.time.Instant.now(),
            syncStatus = "synced",
            syncError = null,
            createdAt = java.time.Instant.now(),
        )
        return CoinCatalogCandidate(
            externalReference = ref,
            title = title,
            countryOrIssuer = issuer?.name,
            denomination = value?.text,
            yearStart = minYear,
            yearEnd = maxYear,
            composition = composition?.text,
            weightGrams = weight,
            diameterMm = size,
            obverseDescription = obverse?.description,
            reverseDescription = reverse?.description,
            historicalContext = comments,
            thumbnailUrl = obverse?.thumbnail,
            numistaUrl = url,
            obverseLettering = obverse?.lettering,
            reverseLettering = reverse?.lettering,
            designers = (obverse?.engravers ?: emptyList()) + (reverse?.engravers ?: emptyList()),
        )
    }
}

@Serializable
data class NumistaTypesSearchResponse(
    val types: List<NumistaTypeSummary>? = null,
)

@Serializable
data class NumistaTypeSummary(
    val id: Int,
    val title: String? = null,
    val issuer: NumistaIssuer? = null,
    @SerialName("min_year") val minYear: Int? = null,
    @SerialName("max_year") val maxYear: Int? = null,
    @SerialName("object_type") val objectType: NumistaObjectType? = null,
    @SerialName("obverse_thumbnail") val obverseThumbnail: String? = null,
    @SerialName("reverse_thumbnail") val reverseThumbnail: String? = null,
)

@Serializable
data class NumistaObjectType(
    val id: String? = null,
    val name: String? = null,
)

@Serializable
data class NumistaIssuersResponse(
    val count: Int? = null,
    val issuers: List<NumistaIssuerEntry>? = null,
)

@Serializable
data class NumistaIssuerEntry(
    val code: String,
    val name: String,
)

@Serializable
data class NumistaIssuer(
    val name: String? = null,
    val code: String? = null,
)

@Serializable
data class NumistaComposition(
    val text: String? = null,
)

@Serializable
data class NumistaSide(
    val description: String? = null,
    val lettering: String? = null,
    val engravers: List<String>? = null,
    val thumbnail: String? = null,
)

@Serializable
data class NumistaValue(
    val text: String? = null,
    @SerialName("numeric_value")
    val numericValue: Double? = null,
    val currency: NumistaCurrency? = null,
)

@Serializable
data class NumistaCurrency(
    val name: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
)

@Serializable
data class NumistaEdge(
    val description: String? = null,
)

@Serializable
data class NumistaTypeDetail(
    val id: Int,
    val url: String? = null,
    val title: String? = null,
    val issuer: NumistaIssuer? = null,
    val value: NumistaValue? = null,
    @SerialName("min_year")
    val minYear: Int? = null,
    @SerialName("max_year")
    val maxYear: Int? = null,
    val composition: NumistaComposition? = null,
    val weight: Double? = null,
    val size: Double? = null,
    val thickness: Double? = null,
    val edge: NumistaEdge? = null,
    val obverse: NumistaSide? = null,
    val reverse: NumistaSide? = null,
    val comments: String? = null,
)
