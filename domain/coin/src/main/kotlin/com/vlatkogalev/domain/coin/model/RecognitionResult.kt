package com.vlatkogalev.domain.coin.model

data class RecognitionResult(
    val overallConfidence: Confidence,
    val countryOrIssuer: String?,
    val denomination: String?,
    val seriesName: String?,
    val year: Int?,
    val era: String? = null,
    val mintMark: String?,
    val metalComposition: String?,
    val estimatedGrade: String?,
    val estimatedGradeValue: String?,
    val rarityQualitative: String?,
    val valueLow: Double?,
    val valueHigh: Double?,
    val mintage: Long?,
    val obverseDescription: String?,
    val reverseDescription: String?,
    // Specifications
    val weightGrams: Double? = null,
    val diameterMm: Double? = null,
    val thicknessMm: Double? = null,
    val edge: String? = null,
    val designerObverse: String? = null,
    val designerReverse: String? = null,
    // Condition
    val positiveFeatures: List<String> = emptyList(),
    val negativeFeatures: List<String> = emptyList(),
    // Market
    val supplySummary: String? = null,
    val demandSummary: String? = null,
    // Design lettering
    val obverseLettering: String? = null,
    val reverseLettering: String? = null,
    val analysisNotes: String? = null,
    val historicalContext: String?,
    val rawJson: String,
) {
    fun toFingerprint(): CoinFingerprint =
        CoinFingerprint(
            countryOrIssuer = countryOrIssuer,
            denomination = denomination,
            seriesName = seriesName,
            year = year,
            mintMark = mintMark,
        )
}
