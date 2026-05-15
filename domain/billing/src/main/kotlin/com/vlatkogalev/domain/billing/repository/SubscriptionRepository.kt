package com.vlatkogalev.domain.billing.repository

import com.vlatkogalev.domain.billing.model.Subscription
import com.vlatkogalev.domain.billing.model.SubscriptionPlan
import com.vlatkogalev.domain.billing.model.SubscriptionStatus
import java.time.Instant
import java.util.UUID

interface SubscriptionRepository {
    fun findByRevenueCatCustomerId(revenueCatCustomerId: String): Subscription?

    fun findByUserId(userId: UUID): Subscription?

    fun updateFromRevenueCat(
        revenueCatCustomerId: String,
        status: SubscriptionStatus,
        expiresAt: Instant?,
        plan: SubscriptionPlan? = null,
    ): Boolean
}
