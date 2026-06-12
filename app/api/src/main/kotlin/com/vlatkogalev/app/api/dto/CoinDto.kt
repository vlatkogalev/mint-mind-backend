package com.vlatkogalev.app.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class SaveCoinRequest(
    val obverseKey: String,
    val reverseKey: String,
    val recognitionResult: RecognitionResultDto,
    val catalogueNumbers: List<CatalogueNumberDto>,
    val notes: String? = null,
) {
    fun validate(): String? {
        if (obverseKey.isBlank()) return "obverseKey is required"
        if (reverseKey.isBlank()) return "reverseKey is required"
        if (recognitionResult.rawJson.isBlank()) return "recognitionResult.rawJson is required"
        if (catalogueNumbers.any { it.catalogueName.isBlank() }) return "catalogueNumbers catalogueName must not be blank"
        return null
    }
}

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
    val recognitionResult: RecognitionResultDto,
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
