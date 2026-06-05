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
    val valueLow: Double?,
    val valueHigh: Double?,
    val mintage: Long?,
    val obverseDescription: String?,
    val reverseDescription: String?,
    val historicalContext: String?,
    val rawJson: String,
) {
    fun toFingerprint(title: String? = null): CoinFingerprint =
        CoinFingerprint(
            countryOrIssuer = countryOrIssuer,
            denomination = denomination,
            seriesName = seriesName,
            title = title,
            year = year,
            mintMark = mintMark,
        )
}
