package com.vlatkogalev.domain.coin.model

data class RecognitionResult(
    val overallConfidence: Confidence,
    val countryOrIssuer: String?,
    val denomination: String?,
    val seriesName: String?,
    val year: Int?,
    val mintMark: String?,
    val metalComposition: String?,
    val estimatedGrade: String?,
    val estimatedGradeValue: String?,
    val rarityQualitative: String?,
    val valueLowUsd: Double?,
    val valueHighUsd: Double?,
    val obverseDescription: String?,
    val reverseDescription: String?,
    val historicalContext: String?,
    val rawJson: String,
)
