package com.vlatkogalev.domain.billing.model

import java.time.Instant

data class RevenueCatSubscriptionEvent(
    val revenueCatCustomerId: String,
    val type: RevenueCatEventType,
    val expiresAt: Instant?,
    val plan: SubscriptionPlan?,
)