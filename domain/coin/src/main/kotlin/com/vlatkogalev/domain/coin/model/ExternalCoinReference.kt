package com.vlatkogalev.domain.coin.model

import java.util.UUID
import java.time.Instant

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
