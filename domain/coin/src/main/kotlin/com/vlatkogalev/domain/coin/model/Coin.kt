package com.vlatkogalev.domain.coin.model

import java.time.Instant
import java.util.UUID

data class Coin(
    val id: UUID,
    val userId: UUID,
    val obverseKey: String,
    val reverseKey: String,
    val recognitionResult: RecognitionResult,
    val catalogueNumbers: List<CatalogueNumber>,
    val setId: UUID?,
    val catalogCoinId: UUID? = null,
    val notes: String?,
    val createdAt: Instant,
)
