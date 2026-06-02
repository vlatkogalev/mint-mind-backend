package com.vlatkogalev.domain.coin.model

import java.time.Instant
import java.util.UUID

data class ExternalCoinReference(
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
