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
    val valueLowUsd: Double? = null,
    val valueHighUsd: Double? = null,
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
)

@Serializable
data class UpdateCoinNotesRequest(
    val notes: String?,
)

@Serializable
data class CoinResponse(
    val id: String,
    val userId: String,
    val obverseKey: String,
    val reverseKey: String,
    val recognitionResult: RecognitionResultDto,
    val catalogueNumbers: List<CatalogueNumberDto>,
    val notes: String?,
    val createdAt: String,
)

@Serializable
data class CollectionStatsResponse(
    val totalCoins: Int,
    val estimatedTotalValueLowUsd: Double,
    val estimatedTotalValueHighUsd: Double,
    val byCountry: Map<String, Int>,
    val byYear: Map<Int, Int>,
)

@Serializable
data class CoinImagesResponse(
    val obverseUrl: String,
    val reverseUrl: String,
)
