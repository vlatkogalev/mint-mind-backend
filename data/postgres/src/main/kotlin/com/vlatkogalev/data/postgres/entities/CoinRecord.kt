package com.vlatkogalev.data.postgres.entities

import java.time.Instant
import java.util.UUID

data class CoinRecord(
    val id: UUID,
    val userId: UUID,
    val obverseKey: String,
    val reverseKey: String,
    val notes: String?,
    val createdAt: Instant,
    val overallConfidence: String,
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
    val setId: UUID?,
    val catalogCoinId: UUID?,
)

data class CatalogueNumberRecord(
    val coinId: UUID,
    val catalogueName: String,
    val number: String?,
    val confidence: String,
)
