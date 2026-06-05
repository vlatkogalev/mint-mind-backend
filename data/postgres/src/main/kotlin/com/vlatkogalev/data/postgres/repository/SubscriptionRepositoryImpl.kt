package com.vlatkogalev.data.postgres.repository

import com.vlatkogalev.data.postgres.tables.SubscriptionsTable
import com.vlatkogalev.domain.billing.model.Subscription
import com.vlatkogalev.domain.billing.model.SubscriptionPlan
import com.vlatkogalev.domain.billing.model.SubscriptionStatus
import com.vlatkogalev.domain.billing.repository.SubscriptionRepository
import com.vlatkogalev.platform.database.dbQuery
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.*
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class SubscriptionRepositoryImpl(
    private val database: R2dbcDatabase,
) : SubscriptionRepository {
    override suspend fun findByRevenueCatCustomerId(revenueCatCustomerId: String): Subscription? =
        dbQuery(database) {
            SubscriptionsTable
                .selectAll()
                .where { SubscriptionsTable.rcCustomerId eq revenueCatCustomerId }
                .firstOrNull()
                ?.toSubscription()
        }

    override suspend fun findByUserId(userId: UUID): Subscription? =
        dbQuery(database) {
            SubscriptionsTable
                .selectAll()
                .where { SubscriptionsTable.userId eq userId }
                .firstOrNull()
                ?.toSubscription()
        }

    override suspend fun updateFromRevenueCat(
        revenueCatCustomerId: String,
        status: SubscriptionStatus,
        expiresAt: Instant?,
        plan: SubscriptionPlan?,
    ): Boolean =
        dbQuery(database) {
            val updated = SubscriptionsTable.update({ SubscriptionsTable.rcCustomerId eq revenueCatCustomerId }) {
                it[SubscriptionsTable.status] = status.name.lowercase()
                it[SubscriptionsTable.expiresAt] = expiresAt?.let { OffsetDateTime.ofInstant(it, ZoneOffset.UTC) }
                if (plan != null) {
                    it[SubscriptionsTable.plan] = plan.name.lowercase()
                }
            }
            updated > 0
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
