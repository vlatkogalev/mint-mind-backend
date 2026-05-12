package com.vlatkogalev.domain.billing

import java.time.Instant
import java.util.UUID

enum class SubscriptionPlan {
    FREE,
    PRO,
    ENTERPRISE,
}

enum class SubscriptionStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED,
}

data class Subscription(
    val id: UUID,
    val userId: UUID,
    val revenueCatCustomerId: String,
    val plan: SubscriptionPlan,
    val status: SubscriptionStatus,
    val expiresAt: Instant?,
)
