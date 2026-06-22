package com.vlatkogalev.data.postgres.entities

import java.time.Instant
import java.util.UUID

data class SubscriptionRecord(
    val id: UUID,
    val userId: UUID,
    val revenueCatCustomerId: String?,
    val plan: String,
    val status: String,
    val expiresAt: Instant?,
)
