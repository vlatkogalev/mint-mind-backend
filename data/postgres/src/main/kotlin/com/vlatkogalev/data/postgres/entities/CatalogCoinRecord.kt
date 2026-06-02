package com.vlatkogalev.data.postgres.entities

import java.time.Instant
import java.util.UUID

data class CatalogCoinRecord(
    val id: UUID,
    val countryOrIssuer: String?,
    val denomination: String?,
    val seriesName: String?,
    val title: String?,
    val year: Int?,
    val mintMark: String?,
    val enrichedAt: Instant?,
    val lastEnrichmentAttemptAt: Instant?,
    val lastEnrichmentFailedAt: Instant?,
    val lastEnrichmentError: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ExternalCoinReferenceRecord(
    val id: UUID,
    val catalogCoinId: UUID,
    val provider: String,
    val externalId: String,
    val externalUrl: String?,
    val lastSyncedAt: Instant?,
    val syncStatus: String?,
    val syncError: String?,
    val createdAt: Instant,
)
