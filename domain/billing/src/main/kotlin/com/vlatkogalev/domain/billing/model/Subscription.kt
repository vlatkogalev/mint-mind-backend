package com.vlatkogalev.domain.billing.model

import java.time.Instant
import java.util.UUID

data class Subscription(
    val id: UUID,
    val userId: UUID,
    val revenueCatCustomerId: String,
    val plan: SubscriptionPlan,
    val status: SubscriptionStatus,
    val expiresAt: Instant?,
)
