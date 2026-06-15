package com.vlatkogalev.domain.coin.model

data class RecognitionResult(
    val overallConfidence: Confidence,
    val countryOrIssuer: String?,
    val denomination: String?,
    val seriesName: String?,
    val year: Int?,
    val era: String? = null,
    // Identification confidence
    val confidenceCountry: String? = null,
    val confidenceDenomination: String? = null,
    val confidenceSeries: String? = null,
    val confidenceYear: String? = null,
    val confidenceEra: String? = null,
    val mintMark: String?,
    // Mint mark detail
    val mintMarkStatus: String? = null,
    val mintMarkConfidence: String? = null,
    val metalComposition: String?,
    val estimatedGrade: String?,
    val estimatedGradeValue: String?,
    val gradeCode: String? = null,
    val gradeConfidence: String? = null,
    val rarityQualitative: String?,
    val rarityScore: Double? = null,
    val valueLow: Double?,
    val valueHigh: Double?,
    val valueCurrency: String? = null,
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
    val valueDisclaimer: String? = null,
    // Design lettering
    val obverseLettering: String? = null,
    val reverseLettering: String? = null,
    val analysisNotes: String? = null,
    val historicalContext: String?,
    // Image analysis
    val obverseVisible: Boolean? = null,
    val reverseVisible: Boolean? = null,
    val imageFocus: String? = null,
    val imageLighting: String? = null,
    val imageResolution: String? = null,
    val imageCropping: String? = null,
    val imageIssues: List<String> = emptyList(),
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
