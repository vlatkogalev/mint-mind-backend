package com.vlatkogalev.domain.coin.model

import java.util.UUID
import java.time.Instant

data class CoinSet(
    val id: UUID,
    val userId: UUID,
    val name: String,
    val description: String?,
    val coinIds: List<UUID>,
    val previewObverseKeys: List<String>,
    val createdAt: Instant,
)
