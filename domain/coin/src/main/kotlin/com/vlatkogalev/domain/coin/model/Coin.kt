package com.vlatkogalev.domain.coin.model

import java.util.UUID
import java.time.Instant

data class Coin(
    val id: UUID,
    val userId: UUID,
    val obverseKey: String,
    val reverseKey: String,
    val recognitionResult: RecognitionResult,
    val catalogueNumbers: List<CatalogueNumber>,
    val setId: UUID?,
    val catalogCoinId: UUID?,
    val notes: String?,
    val createdAt: Instant,
)
