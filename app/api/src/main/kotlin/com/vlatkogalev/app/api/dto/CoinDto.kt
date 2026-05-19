package com.vlatkogalev.app.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CatalogueNumberDto(
    val catalogueName: String,
    val number: String?,
    val confidence: String,
)

@Serializable
data class RecognitionResultDto(
    val overallConfidence: String,
    val countryOrIssuer: String? = null,
    val denomination: String? = null,
    val seriesName: String? = null,
    val year: Int? = null,
    val mintMark: String? = null,
    val metalComposition: String? = null,
    val estimatedGrade: String? = null,
    val estimatedGradeValue: String? = null,
    val rarityQualitative: String? = null,
    val valueLow: Double? = null,
    val valueHigh: Double? = null,
    val mintage: Long? = null,
    val obverseDescription: String? = null,
    val reverseDescription: String? = null,
    val historicalContext: String? = null,
    val rawJson: String,
)

@Serializable
data class SaveCoinRequest(
    val obverseKey: String,
    val reverseKey: String,
    val recognitionResult: RecognitionResultDto,
    val catalogueNumbers: List<CatalogueNumberDto> = emptyList(),
    val notes: String? = null,
) {
    fun validate(): String? {
        if (obverseKey.isBlank()) return "obverseKey is required"
        if (reverseKey.isBlank()) return "reverseKey is required"
        if (recognitionResult.rawJson.isBlank()) return "recognitionResult.rawJson is required"
        if (catalogueNumbers.any { it.catalogueName.isBlank() }) return "catalogueName must not be blank"
        return null
    }
}

@Serializable
data class UpdateCoinNotesRequest(
    val notes: String?,
)

@Serializable
data class CoinSummaryResponse(
    val id: String,
    val obverseKey: String,
    val reverseKey: String,
    val denomination: String?,
    val countryOrIssuer: String?,
    val year: Int?,
    val estimatedGrade: String?,
    val estimatedValueMean: Double?,
    val setId: String?,
    val createdAt: String,
)

@Serializable
data class CoinDetailResponse(
    val id: String,
    val obverseKey: String,
    val reverseKey: String,
    val recognitionResult: RecognitionResultDto,
    val catalogueNumbers: List<CatalogueNumberDto>,
    val setId: String?,
    val notes: String?,
    val createdAt: String,
)

@Serializable
data class CollectionHighlightsResponse(
    val mostValuable: CoinSummaryResponse?,
    val mostAncient: CoinSummaryResponse?,
    val rarest: CoinSummaryResponse?,
)

@Serializable
data class CoinListResponse(
    val coins: List<CoinSummaryResponse>,
    val totalCoins: Int,
    val totalIssuers: Int,
    val estimatedMeanValue: Double,
    val highlights: CollectionHighlightsResponse,
)

@Serializable
data class CoinImagesResponse(
    val obverseUrl: String,
    val reverseUrl: String,
)