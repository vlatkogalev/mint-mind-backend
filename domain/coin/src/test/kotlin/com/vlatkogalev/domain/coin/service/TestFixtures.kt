package com.vlatkogalev.domain.coin.service

import com.vlatkogalev.domain.coin.model.CatalogueNumber
import com.vlatkogalev.domain.coin.model.Coin
import com.vlatkogalev.domain.coin.model.Confidence
import com.vlatkogalev.domain.coin.model.RecognitionResult
import java.time.Instant
import java.util.UUID

object TestFixtures {
    val USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    val OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002")

    fun makeCoin(
        id: UUID = UUID.randomUUID(),
        userId: UUID = USER_ID,
        obverseKey: String = "users/obverse.jpg",
        reverseKey: String = "users/reverse.jpg",
        recognitionResult: RecognitionResult = makeRecognitionResult(),
        catalogueNumbers: List<CatalogueNumber> = emptyList(),
        setId: UUID? = null,
        catalogCoinId: UUID? = null,
        notes: String? = null,
        createdAt: Instant = Instant.now(),
    ): Coin = Coin(
        id = id,
        userId = userId,
        obverseKey = obverseKey,
        reverseKey = reverseKey,
        recognitionResult = recognitionResult,
        catalogueNumbers = catalogueNumbers,
        setId = setId,
        catalogCoinId = catalogCoinId,
        notes = notes,
        createdAt = createdAt,
    )

    fun makeRecognitionResult(
        overallConfidence: Confidence = Confidence.HIGH,
        countryOrIssuer: String? = "United States",
        denomination: String? = "1 Dollar",
        seriesName: String? = null,
        year: Int? = null,
        era: String? = null,
        mintMark: String? = null,
        metalComposition: String? = null,
        obverseDescription: String? = null,
        reverseDescription: String? = null,
        historicalContext: String? = null,
        valueLow: Double? = null,
        valueHigh: Double? = null,
        mintage: Long? = null,
        weightGrams: Double? = null,
        diameterMm: Double? = null,
    ): RecognitionResult = RecognitionResult(
        overallConfidence = overallConfidence,
        countryOrIssuer = countryOrIssuer,
        denomination = denomination,
        seriesName = seriesName,
        year = year,
        era = era,
        mintMark = mintMark,
        metalComposition = metalComposition,
        gradeName = null,
        gradeAbbreviation = null,
        rarityQualitative = null,
        valueLow = valueLow,
        valueHigh = valueHigh,
        mintage = mintage,
        obverseDescription = obverseDescription,
        reverseDescription = reverseDescription,
        historicalContext = historicalContext,
        weightGrams = weightGrams,
        diameterMm = diameterMm,
        rawJson = "{}",
    )

    fun makeCatalogueNumber(
        catalogueName: String = "Krause",
        number: String? = "KM# 110",
        confidence: Confidence = Confidence.HIGH,
    ): CatalogueNumber = CatalogueNumber(
        catalogueName = catalogueName,
        number = number,
        confidence = confidence,
    )
}
