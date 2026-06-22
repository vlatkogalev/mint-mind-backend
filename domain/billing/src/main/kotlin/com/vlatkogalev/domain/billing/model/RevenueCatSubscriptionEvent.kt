package com.vlatkogalev.domain.billing.model

import java.time.Instant
import java.util.UUID

data class RevenueCatSubscriptionEvent(
    val revenueCatCustomerId: String,
    val userId: UUID?,
    val type: RevenueCatEventType,
    val expiresAt: Instant?,
    val plan: SubscriptionPlan?,
)
