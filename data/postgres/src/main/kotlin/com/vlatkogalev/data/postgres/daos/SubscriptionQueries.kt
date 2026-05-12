package com.vlatkogalev.data.postgres.daos

import com.vlatkogalev.data.postgres.entities.SubscriptionRecord
import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

class SubscriptionQueries(
    private val dataSource: DataSource,
) {
    fun findByRevenueCatCustomerId(revenueCatCustomerId: String): SubscriptionRecord? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, user_id, rc_customer_id, plan, status, expires_at
                FROM subscriptions
                WHERE rc_customer_id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, revenueCatCustomerId)
                statement.executeQuery().use { rs -> if (rs.next()) rs.toSubscriptionRecord() else null }
            }
        }

    fun findByUserId(userId: UUID): SubscriptionRecord? =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT id, user_id, rc_customer_id, plan, status, expires_at
                FROM subscriptions
                WHERE user_id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setObject(1, userId)
                statement.executeQuery().use { rs -> if (rs.next()) rs.toSubscriptionRecord() else null }
            }
        }

    fun updateFromRevenueCat(
        revenueCatCustomerId: String,
        status: String,
        expiresAt: Instant?,
        plan: String?,
    ): Boolean =
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                UPDATE subscriptions
                SET status = ?,
                    expires_at = ?,
                    plan = COALESCE(?, plan)
                WHERE rc_customer_id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, status)
                statement.setObject(2, expiresAt?.let { OffsetDateTime.ofInstant(it, ZoneOffset.UTC) })
                statement.setString(3, plan)
                statement.setString(4, revenueCatCustomerId)
                statement.executeUpdate() > 0
            }
        }

    private fun ResultSet.toSubscriptionRecord(): SubscriptionRecord =
        SubscriptionRecord(
            id = getObject("id", UUID::class.java),
            userId = getObject("user_id", UUID::class.java),
            revenueCatCustomerId = getString("rc_customer_id"),
            plan = getString("plan"),
            status = getString("status"),
            expiresAt = getObject("expires_at", OffsetDateTime::class.java)?.toInstant(),
        )
}
