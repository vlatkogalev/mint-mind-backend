package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.daos.SubscriptionQueries
import com.vlatkogalev.data.postgres.entities.SubscriptionRecord
import com.vlatkogalev.domain.billing.Subscription
import com.vlatkogalev.domain.billing.SubscriptionPlan
import com.vlatkogalev.domain.billing.SubscriptionRepository
import com.vlatkogalev.domain.billing.SubscriptionStatus
import java.time.Instant
import java.util.UUID

class SubscriptionRepositoryImpl(
    private val queries: SubscriptionQueries,
) : SubscriptionRepository {
    override fun findByRevenueCatCustomerId(revenueCatCustomerId: String): Subscription? =
        queries.findByRevenueCatCustomerId(revenueCatCustomerId)?.toSubscription()

    override fun findByUserId(userId: UUID): Subscription? =
        queries.findByUserId(userId)?.toSubscription()

    override fun updateFromRevenueCat(
        revenueCatCustomerId: String,
        status: SubscriptionStatus,
        expiresAt: Instant?,
        plan: SubscriptionPlan?,
    ): Boolean =
        queries.updateFromRevenueCat(
            revenueCatCustomerId = revenueCatCustomerId,
            status = status.name.lowercase(),
            expiresAt = expiresAt,
            plan = plan?.name?.lowercase(),
        )

    private fun SubscriptionRecord.toSubscription(): Subscription =
        Subscription(
            id = id,
            userId = userId,
            revenueCatCustomerId = revenueCatCustomerId,
            plan = SubscriptionPlan.valueOf(plan.uppercase()),
            status = SubscriptionStatus.valueOf(status.uppercase()),
            expiresAt = expiresAt,
        )
}
