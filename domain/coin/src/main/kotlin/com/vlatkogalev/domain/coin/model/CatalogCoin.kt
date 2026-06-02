package com.vlatkogalev.domain.coin.model

import java.time.Instant
import java.util.UUID

data class CatalogCoin(
    val id: UUID,
    val fingerprint: CoinFingerprint,
    val enrichedAt: Instant?,
    val lastEnrichmentAttemptAt: Instant?,
    val lastEnrichmentFailedAt: Instant?,
    val lastEnrichmentError: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CoinFingerprint(
    val countryOrIssuer: String?,
    val denomination: String?,
    val seriesName: String?,
    val title: String?,
    val year: Int?,
    val mintMark: String?,
)

fun CoinFingerprint.normalized(): CoinFingerprint =
    copy(
        countryOrIssuer = countryOrIssuer.normalizedPart(),
        denomination = denomination.normalizedPart(),
        seriesName = seriesName.normalizedPart(),
        title = title.normalizedPart(),
        mintMark = mintMark.normalizedPart(),
    )

fun RecognitionResult.toFingerprint(title: String?): CoinFingerprint =
    CoinFingerprint(
        countryOrIssuer = countryOrIssuer,
        denomination = denomination,
        seriesName = seriesName,
        title = title,
        year = year,
        mintMark = mintMark,
    ).normalized()

private fun String?.normalizedPart(): String? = this?.trim()?.ifEmpty { null }
