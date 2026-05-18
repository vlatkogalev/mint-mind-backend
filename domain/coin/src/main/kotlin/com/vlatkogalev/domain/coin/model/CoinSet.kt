package com.vlatkogalev.domain.coin.model

import java.time.Instant
import java.util.UUID

data class CoinSet(
    val id: UUID,
    val userId: UUID,
    val name: String,
    val description: String?,
    val coinIds: List<UUID>,
    val createdAt: Instant,
)
