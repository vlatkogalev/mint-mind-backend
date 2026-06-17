package com.vlatkogalev.data.numista

import com.vlatkogalev.domain.coin.model.CoinCatalogCandidate
import com.vlatkogalev.domain.coin.model.CoinFingerprint
import com.vlatkogalev.domain.coin.model.ExternalCoinReference
import com.vlatkogalev.domain.coin.service.CoinCatalogProvider
import com.vlatkogalev.platform.core.Result
import com.vlatkogalev.platform.core.config.NumistaConfig
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

    override suspend fun findCandidates(fingerprint: CoinFingerprint): Result<List<CoinCatalogCandidate>> =
        try {
            if (!config.enabled) {
                return Result.Success(emptyList())
            }

            val query = buildQuery(fingerprint)
            if (query.isBlank()) return Result.Success(emptyList())

            val searchResponse: NumistaTypesSearchResponse = client
                .get("${config.baseUrl}/v3/types") {
                    parameter("category", "coin")
                    parameter("q", query)
                    parameter("count", 10)
                    header("Numista-API-Key", config.apiKey)
                }.body()

            val summaries = searchResponse.types ?: emptyList()

            coroutineScope {
                summaries.map { summary ->
                    async {
                        try {
                            val detail: NumistaTypeDetail = client
                                .get("${config.baseUrl}/v3/types/${summary.id}") {
                                    header("Numista-API-Key", config.apiKey)
                                }.body()
                            detail.toCandidate(summary.id)
                        } catch (_: Exception) {
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
            fingerprint.seriesName,
            fingerprint.year?.toString(),
            fingerprint.mintMark,
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
)

@Serializable
data class NumistaIssuer(
    val name: String? = null,
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
