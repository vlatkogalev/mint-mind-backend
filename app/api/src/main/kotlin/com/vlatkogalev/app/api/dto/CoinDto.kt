package com.vlatkogalev.app.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class SaveCoinRequest(
    val obverseKey: String,
    val reverseKey: String,
    val coinData: CoinDataDto,
    val aiAnalysis: AiAnalysisDto,
    val catalogueNumbers: List<CatalogueNumberDto>,
    val notes: String? = null,
) {
    fun validate(): String? {
        if (obverseKey.isBlank()) return "obverseKey is required"
        if (reverseKey.isBlank()) return "reverseKey is required"
        if (aiAnalysis.rawJson.isBlank()) return "aiAnalysis.rawJson is required"
        if (catalogueNumbers.any { it.catalogueName.isBlank() }) return "catalogueNumbers catalogueName must not be blank"
        return null
    }
}

@Serializable
data class CoinDataDto(
    val countryOrIssuer: String? = null,
    val denomination: String? = null,
    val seriesName: String? = null,
    val year: Int? = null,
    val era: String? = null,
    val mintMark: String? = null,
    val metalComposition: String? = null,
    val weightGrams: Double? = null,
    val diameterMm: Double? = null,
    val thicknessMm: Double? = null,
    val edge: String? = null,
    val obverseDescription: String? = null,
    val reverseDescription: String? = null,
    val historicalContext: String? = null,
    val obverseLettering: String? = null,
    val reverseLettering: String? = null,
    val designerObverse: String? = null,
    val designerReverse: String? = null,
    val mintage: Long? = null,
    val shape: String? = null,
    val technique: String? = null,
    val orientation: String? = null,
    val mintName: String? = null,
    val ruler: String? = null,
    val objectType: String? = null,
    val demonetized: Boolean? = null,
    val tags: List<String> = emptyList(),
    val numistaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val minYear: Int? = null,
    val maxYear: Int? = null,
)

@Serializable
data class AiAnalysisDto(
    val overallConfidence: String,
    val confidenceCountry: String? = null,
    val confidenceDenomination: String? = null,
    val confidenceSeries: String? = null,
    val confidenceYear: String? = null,
    val confidenceEra: String? = null,
    val mintMarkStatus: String? = null,
    val mintMarkConfidence: String? = null,
    val estimatedGrade: String? = null,
    val estimatedGradeValue: String? = null,
    val gradeCode: String? = null,
    val gradeConfidence: String? = null,
    val rarityQualitative: String? = null,
    val rarityScore: Double? = null,
    val valueLow: Double? = null,
    val valueHigh: Double? = null,
    val valueCurrency: String? = null,
    val positiveFeatures: List<String> = emptyList(),
    val negativeFeatures: List<String> = emptyList(),
    val supplySummary: String? = null,
    val demandSummary: String? = null,
    val valueDisclaimer: String? = null,
    val analysisNotes: String? = null,
    val obverseVisible: Boolean? = null,
    val reverseVisible: Boolean? = null,
    val imageFocus: String? = null,
    val imageLighting: String? = null,
    val imageResolution: String? = null,
    val imageCropping: String? = null,
    val imageIssues: List<String> = emptyList(),
    val rawJson: String,
)

@Serializable
data class CatalogueNumberDto(
    val catalogueName: String,
    val number: String? = null,
    val confidence: String,
)

@Serializable
data class UpdateCoinNotesRequest(
    val notes: String? = null,
)

@Serializable
data class CoinDetailResponse(
    val id: String,
    val userId: String,
    val obverseUrl: String,
    val reverseUrl: String,
    val coinData: CoinDataDto,
    val aiAnalysis: AiAnalysisDto,
    val catalogueNumbers: List<CatalogueNumberDto>,
    val setId: String?,
    val catalogCoinId: String?,
    val notes: String?,
    val createdAt: Long,
)

@Serializable
data class CoinSummaryResponse(
    val id: String,
    val obverseUrl: String,
    val reverseUrl: String,
    val denomination: String?,
    val countryOrIssuer: String?,
    val year: Int?,
    val mintage: Long?,
    val estimatedGrade: String?,
    val estimatedGradeValue: String?,
    val estimatedValueMean: Double?,
    val setId: String?,
    val createdAt: Long,
)

@Serializable
data class CoinListResponse(
    val coins: List<CoinSummaryResponse>,
    val nextCursor: Long?,
)

@Serializable
data class CoinImagesResponse(
    val obverseUrl: String,
    val reverseUrl: String,
)

@Serializable
data class CreateCoinSetRequest(
    val name: String,
    val description: String? = null,
) {
    fun validate(): String? {
        if (name.isBlank()) return "name is required"
        if (name.length > 255) return "name must be 255 characters or fewer"
        return null
    }
}

@Serializable
data class UpdateCoinSetRequest(
    val name: String,
    val description: String? = null,
) {
    fun validate(): String? {
        if (name.isBlank()) return "name is required"
        if (name.length > 255) return "name must be 255 characters or fewer"
        return null
    }
}

@Serializable
data class ModifySetCoinsRequest(
    val coinIds: List<String>,
) {
    fun validate(): String? {
        if (coinIds.isEmpty()) return "coinIds must not be empty"
        return null
    }
}

@Serializable
data class CoinCollectionStatsResponse(
    val totalCoins: Int,
    val totalIssuers: Int,
    val estimatedTotalValueMean: Double,
    val highlights: CollectionHighlightsResponse,
)

@Serializable
data class CollectionHighlightsResponse(
    val mostValuable: CoinDetailResponse?,
    val mostAncient: CoinDetailResponse?,
    val rarest: CoinDetailResponse?,
)

@Serializable
data class CoinSetResponse(
    val id: String,
    val name: String,
    val description: String?,
    val previewObverseUrls: List<String>,
    val coinCount: Int,
    val createdAt: Long,
)

@Serializable
data class MatchResultDto(
    val tier: String,
    val bestCandidate: MatchCandidateDto? = null,
    val allCandidates: List<MatchCandidateDto> = emptyList(),
    val retrievalKey: String,
)

@Serializable
data class MatchCandidateDto(
    val catalogCoinId: String?,
    val providerName: String,
    val externalId: String?,
    val score: Int,
    val scoreBreakdown: Map<String, Int>,
    val dataCompleteness: Map<String, Boolean> = emptyMap(),
)

@Serializable
data class DebugNumistaMatchRequest(
    val coinData: CoinDataDto,
    val aiAnalysis: AiAnalysisDto,
)

@Serializable
data class MetricsResponseDto(
    val attemptsTotal: Long,
    val matchedTotal: Long,
    val ambiguousTotal: Long,
    val noMatchTotal: Long,
    val numistaCallsTotal: Long,
    val cacheHitsTotal: Long,
    val avgCandidatesPerMatch: Double,
)
