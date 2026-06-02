package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.domain.billing.model.Subscription
import com.vlatkogalev.domain.billing.model.SubscriptionPlan
import com.vlatkogalev.domain.billing.model.SubscriptionStatus
import com.vlatkogalev.domain.billing.repository.SubscriptionRepository
import com.vlatkogalev.platform.database.tables.SubscriptionsTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class SubscriptionRepositoryImpl : SubscriptionRepository {
    override suspend fun findByRevenueCatCustomerId(revenueCatCustomerId: String): Subscription? =
        newSuspendedTransaction {
            SubscriptionsTable.selectAll()
                .where { SubscriptionsTable.rcCustomerId eq revenueCatCustomerId }
                .singleOrNull()
                ?.toSubscription()
        }

    override suspend fun findByUserId(userId: UUID): Subscription? =
        newSuspendedTransaction {
            SubscriptionsTable.selectAll()
                .where { SubscriptionsTable.userId eq userId }
                .singleOrNull()
                ?.toSubscription()
        }

    override suspend fun updateFromRevenueCat(
        revenueCatCustomerId: String,
        status: SubscriptionStatus,
        expiresAt: Instant?,
        plan: SubscriptionPlan?,
    ): Boolean =
        newSuspendedTransaction {
            SubscriptionsTable.update(
                where = { SubscriptionsTable.rcCustomerId eq revenueCatCustomerId },
                body = {
                    it[SubscriptionsTable.status] = status.name.lowercase()
                    if (expiresAt != null) it[SubscriptionsTable.expiresAt] =
                        OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC)
                    if (plan != null) it[SubscriptionsTable.plan] = plan.name.lowercase()
                },
            ) > 0
        }

    private fun ResultRow.toSubscription(): Subscription =
        Subscription(
            id = this[SubscriptionsTable.id],
            userId = this[SubscriptionsTable.userId],
            revenueCatCustomerId = this[SubscriptionsTable.rcCustomerId],
            plan = SubscriptionPlan.valueOf(this[SubscriptionsTable.plan].uppercase()),
            status = SubscriptionStatus.valueOf(this[SubscriptionsTable.status].uppercase()),
            expiresAt = this[SubscriptionsTable.expiresAt]?.toInstant(),
        )
}
