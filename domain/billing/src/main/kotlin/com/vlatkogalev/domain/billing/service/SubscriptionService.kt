package com.vlatkogalev.domain.billing.service

import com.vlatkogalev.domain.billing.model.RevenueCatEventType
import com.vlatkogalev.domain.billing.model.RevenueCatSubscriptionEvent
import com.vlatkogalev.domain.billing.model.SubscriptionPlan
import com.vlatkogalev.domain.billing.model.SubscriptionStatus
import com.vlatkogalev.domain.billing.repository.SubscriptionRepository
import com.vlatkogalev.platform.core.Result
import java.time.Instant
import java.util.UUID

class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
) {
    suspend fun getUserPlan(userId: UUID, now: Instant = Instant.now()): SubscriptionPlan =
        subscriptionRepository.findByUserId(userId)
            ?.takeIf { it.status == SubscriptionStatus.ACTIVE }
            ?.takeIf { it.expiresAt == null || it.expiresAt.isAfter(now) }
            ?.plan
            ?: SubscriptionPlan.FREE

    suspend fun handleRevenueCatEvent(event: RevenueCatSubscriptionEvent): Result<Unit> =
        try {
            val updated = subscriptionRepository.upsertByRevenueCatCustomerId(
                userId = event.userId,
                revenueCatCustomerId = event.revenueCatCustomerId,
                status = event.type.toSubscriptionStatus(),
                expiresAt = event.expiresAt,
                plan = event.plan,
            )

            if (updated) Result.Success(Unit) else Result.Failure("Subscription not found")
        } catch (ex: Exception) {
            Result.Failure(ex.message ?: "Failed to process RevenueCat event", ex)
        }

    private fun RevenueCatEventType.toSubscriptionStatus(): SubscriptionStatus =
        when (this) {
            RevenueCatEventType.INITIAL_PURCHASE,
            RevenueCatEventType.RENEWAL,
            RevenueCatEventType.UNCANCELLATION,
                -> SubscriptionStatus.ACTIVE

            RevenueCatEventType.CANCELLATION -> SubscriptionStatus.CANCELLED
            RevenueCatEventType.EXPIRATION -> SubscriptionStatus.EXPIRED
        }
}
