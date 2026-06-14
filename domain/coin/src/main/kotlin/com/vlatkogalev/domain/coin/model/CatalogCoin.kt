package com.vlatkogalev.domain.coin.model

import java.util.UUID
import java.time.Instant

data class CatalogCoin(
    val id: UUID,
    val fingerprint: CoinFingerprint,
    val title: String? = null,
    val composition: String? = null,
    val weightGrams: Double? = null,
    val diameterMm: Double? = null,
    val obverseDescription: String? = null,
    val reverseDescription: String? = null,
    val historicalContext: String? = null,
    val thumbnailUrl: String? = null,
    val numistaUrl: String? = null,
    val enrichedAt: Instant?,
    val lastEnrichmentAttemptAt: Instant?,
    val lastEnrichmentFailedAt: Instant?,
    val lastEnrichmentError: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
