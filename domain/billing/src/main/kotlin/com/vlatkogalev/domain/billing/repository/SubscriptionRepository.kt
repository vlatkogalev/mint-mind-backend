package com.vlatkogalev.domain.billing.repository

import com.vlatkogalev.domain.billing.model.Subscription
import com.vlatkogalev.domain.billing.model.SubscriptionPlan
import com.vlatkogalev.domain.billing.model.SubscriptionStatus
import java.time.Instant
import java.util.UUID

interface SubscriptionRepository {
    suspend fun findByRevenueCatCustomerId(revenueCatCustomerId: String): Subscription?

    suspend fun findByUserId(userId: UUID): Subscription?

    suspend fun updateFromRevenueCat(
        revenueCatCustomerId: String,
        status: SubscriptionStatus,
        expiresAt: Instant?,
        plan: SubscriptionPlan? = null,
    ): Boolean

    /**
     * Links/updates a subscription by its RevenueCat customer id. If no row matches the
     * customer id yet, falls back to the user's existing seeded row (with a null customer id)
     * and links it to [revenueCatCustomerId]. Returns true when a row was updated.
     */
    suspend fun upsertByRevenueCatCustomerId(
        userId: UUID?,
        revenueCatCustomerId: String,
        status: SubscriptionStatus,
        expiresAt: Instant?,
        plan: SubscriptionPlan? = null,
    ): Boolean
}
